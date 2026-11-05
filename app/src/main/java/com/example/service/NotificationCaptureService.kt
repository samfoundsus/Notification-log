package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.db.AppDatabase
import com.example.data.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class RecentSignature(
        val packageName: String,
        val title: String,
        val text: String,
        val savedAtMillis: Long
    )

    private val recentLock = Any()
    private val recentEntries = ArrayDeque<RecentSignature>(MAX_RECENT_ENTRIES)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return

            val pkg = sbn.packageName?.takeIf { it.isNotBlank() } ?: return
            if (pkg == applicationContext.packageName) return

            val notification = sbn.notification ?: return
            val flags = notification.flags

            // Skip ongoing, foreground service, and group summary notifications
            if (sbn.isOngoing) return
            if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return
            if ((flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return
            if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

            val extras: Bundle? = notification.extras
            val title = extractTitle(extras)
            val text = extractText(extras)

            // Skip notifications with both title and text empty
            if (title.isEmpty() && text.isEmpty()) return

            val now = System.currentTimeMillis()
            if (isDuplicateWithinWindow(pkg, title, text, now)) return

            val postTime = if (sbn.postTime > 0L) sbn.postTime else now

            serviceScope.launch {
                try {
                    val resolvedAppName = resolveAppName(pkg)
                    val entity = NotificationEntity(
                        packageName = pkg,
                        appName = resolvedAppName,
                        title = title,
                        text = text,
                        timestamp = postTime
                    )
                    AppDatabase.getInstance(applicationContext)
                        .notificationDao()
                        .insert(entity)
                } catch (_: Throwable) {
                    // Never allow database or PackageManager errors to crash the listener service
                }
            }
        } catch (_: Throwable) {
            // Never allow malformed notification extras to crash the listener service
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            requestRebind(ComponentName(this, NotificationCaptureService::class.java))
        } catch (_: Throwable) {
            // Ignore system rebind exceptions
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun extractTitle(extras: Bundle?): String {
        if (extras == null) return ""
        val primary = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        if (!primary.isNullOrEmpty()) return primary
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
        return bigTitle.orEmpty()
    }

    private fun extractText(extras: Bundle?): String {
        if (extras == null) return ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (!bigText.isNullOrEmpty()) return bigText
        val regularText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (!regularText.isNullOrEmpty()) return regularText
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        return subText.orEmpty()
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString().trim().ifEmpty { packageName }
        } catch (_: Throwable) {
            packageName
        }
    }

    private fun isDuplicateWithinWindow(
        packageName: String,
        title: String,
        text: String,
        nowMillis: Long
    ): Boolean {
        synchronized(recentLock) {
            recentEntries.removeAll { nowMillis - it.savedAtMillis > DUPLICATE_WINDOW_MS }
            val duplicate = recentEntries.any {
                it.packageName == packageName &&
                    it.title == title &&
                    it.text == text &&
                    (nowMillis - it.savedAtMillis) <= DUPLICATE_WINDOW_MS
            }
            if (duplicate) return true

            if (recentEntries.size >= MAX_RECENT_ENTRIES) {
                recentEntries.removeFirst()
            }
            recentEntries.addLast(
                RecentSignature(
                    packageName = packageName,
                    title = title,
                    text = text,
                    savedAtMillis = nowMillis
                )
            )
            return false
        }
    }

    companion object {
        private const val DUPLICATE_WINDOW_MS = 5_000L
        private const val MAX_RECENT_ENTRIES = 20
    }
}
