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

    private val recentKeyLock = Any()
    private val lastContentHashByKey = object : LinkedHashMap<String, Int>(MAX_TRACKED_KEYS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?): Boolean {
            return size > MAX_TRACKED_KEYS
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            try {
                val activeList = getActiveNotifications() ?: return@launch
                val sorted = activeList.sortedBy { it.postTime }
                for (sbn in sorted) {
                    try {
                        captureAndStore(sbn)
                    } catch (_: Throwable) {
                        // Never allow one bad notification to stop importing the rest
                    }
                }
            } catch (_: Throwable) {
                // Ignore if system refuses getActiveNotifications
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        serviceScope.launch {
            try {
                captureAndStore(sbn)
            } catch (_: Throwable) {
                // Never allow a malformed notification to crash the service
            }
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

    private suspend fun captureAndStore(sbn: StatusBarNotification) {
        val pkg = sbn.packageName?.takeIf { it.isNotBlank() } ?: return
        if (pkg == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras: Bundle? = notification.extras

        val resolvedAppName = resolveAppName(pkg)
        val title = extractTitle(extras, resolvedAppName)
        val text = extractText(extras, notification)

        val key = sbn.key ?: "${pkg}_${sbn.id}_${sbn.tag.orEmpty()}"
        if (isUnchangedForKey(key, title, text)) return

        val postTime = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis()
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
    }

    private fun extractTitle(extras: Bundle?, fallbackAppName: String): String {
        if (extras != null) {
            val candidates = arrayOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_TITLE_BIG,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_SUB_TEXT
            )
            for (extraKey in candidates) {
                val value = safeCharSequenceToString(extras, extraKey)
                if (value.isNotEmpty()) return value
            }
        }
        return fallbackAppName.ifEmpty { FALLBACK_NO_TEXT }
    }

    private fun extractText(extras: Bundle?, notification: Notification): String {
        if (extras != null) {
            val bigText = safeCharSequenceToString(extras, Notification.EXTRA_BIG_TEXT)
            if (bigText.isNotEmpty()) return bigText

            val latestMessage = extractLatestMessageFromBundleArray(extras)
            if (latestMessage.isNotEmpty()) return latestMessage

            val textLines = extractJoinedTextLines(extras)
            if (textLines.isNotEmpty()) return textLines

            val regularText = safeCharSequenceToString(extras, Notification.EXTRA_TEXT)
            if (regularText.isNotEmpty()) return regularText

            val summaryText = safeCharSequenceToString(extras, Notification.EXTRA_SUMMARY_TEXT)
            if (summaryText.isNotEmpty()) return summaryText

            val infoText = safeCharSequenceToString(extras, Notification.EXTRA_INFO_TEXT)
            if (infoText.isNotEmpty()) return infoText
        }

        val ticker = try {
            notification.tickerText?.toString()?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
        if (ticker.isNotEmpty()) return ticker

        return FALLBACK_NO_TEXT
    }

    @Suppress("DEPRECATION")
    private fun extractLatestMessageFromBundleArray(extras: Bundle): String {
        return try {
            val parcelables = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return ""
            for (i in parcelables.indices.reversed()) {
                val bundle = parcelables[i] as? Bundle ?: continue
                val msgText = bundle.getCharSequence("text")?.toString()?.trim().orEmpty()
                val sender = bundle.getCharSequence("sender")?.toString()?.trim().orEmpty()
                if (msgText.isNotEmpty() && sender.isNotEmpty()) {
                    return "$sender: $msgText"
                } else if (msgText.isNotEmpty()) {
                    return msgText
                } else if (sender.isNotEmpty()) {
                    return sender
                }
            }
            ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun extractJoinedTextLines(extras: Bundle): String {
        return try {
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) ?: return ""
            lines.mapNotNull { cs ->
                cs?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }.joinToString(separator = "\n")
        } catch (_: Throwable) {
            ""
        }
    }

    private fun safeCharSequenceToString(extras: Bundle, key: String): String {
        return try {
            extras.getCharSequence(key)?.toString()?.trim().orEmpty()
        } catch (_: Throwable) {
            ""
        }
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

    private fun isUnchangedForKey(key: String, title: String, text: String): Boolean {
        val contentHash = 31 * title.hashCode() + text.hashCode()
        synchronized(recentKeyLock) {
            val previousHash = lastContentHashByKey[key]
            if (previousHash != null && previousHash == contentHash) {
                return true
            }
            lastContentHashByKey[key] = contentHash
            return false
        }
    }

    companion object {
        private const val MAX_TRACKED_KEYS = 100
        private const val FALLBACK_NO_TEXT = "(no text)"
    }
}
