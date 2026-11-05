package com.example.service

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
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
    private val lastContentHashByKey =
        object : LinkedHashMap<String, Int>(MAX_TRACKED_KEYS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?): Boolean {
                return size > MAX_TRACKED_KEYS
            }
        }

    private val latestMessageLock = Any()
    private val latestMessageTimestampByKey =
        object : LinkedHashMap<String, Long>(MAX_TRACKED_KEYS, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                return size > MAX_TRACKED_KEYS
            }
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch {
            try {
                val activeList = getActiveNotifications() ?: return@launch
                rebuildActiveContentIntents(activeList)
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
        val key = resolveNotificationKey(sbn)
        updateActiveContentIntent(key, sbn.notification?.contentIntent)
        serviceScope.launch {
            try {
                captureAndStore(sbn)
            } catch (_: Throwable) {
                // Never allow a malformed notification to crash the service
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        if (sbn == null) return
        val key = resolveNotificationKey(sbn)
        removeActiveContentIntent(key)
        val removedAt = System.currentTimeMillis()
        val reasonLabel = mapRemovalReason(reason)
        serviceScope.launch {
            try {
                AppDatabase.getInstance(applicationContext)
                    .notificationDao()
                    .markRemovedByKey(
                        notificationKey = key,
                        removedAt = removedAt,
                        removalReason = reasonLabel
                    )
            } catch (_: Throwable) {
                // Never allow database errors to crash the listener service
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        onNotificationRemoved(sbn, null, 0)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        clearActiveContentIntents()
        try {
            requestRebind(ComponentName(this, NotificationCaptureService::class.java))
        } catch (_: Throwable) {
            // Ignore system rebind exceptions
        }
    }

    override fun onDestroy() {
        clearActiveContentIntents()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun captureAndStore(sbn: StatusBarNotification) {
        val pkg = sbn.packageName?.takeIf { it.isNotBlank() } ?: return
        if (pkg == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val key = resolveNotificationKey(sbn)
        updateActiveContentIntent(key, notification.contentIntent)

        val extras: Bundle? = notification.extras
        val resolvedAppName = resolveAppName(pkg)
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 || sbn.isOngoing
        val postTime = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis()

        val messagingStyle = try {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        } catch (_: Throwable) {
            null
        }

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            captureMessagingStyleNotification(
                pkg = pkg,
                resolvedAppName = resolvedAppName,
                key = key,
                isOngoing = isOngoing,
                postTime = postTime,
                extras = extras,
                messagingStyle = messagingStyle
            )
            return
        }

        val title = extractTitle(extras, resolvedAppName)
        val text = extractText(extras, notification)

        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
        if (!hasTrackedHashForKey(key)) {
            val latestStored = dao.getLatestByKey(key)
            if (latestStored != null) {
                seedHashForKey(key, latestStored.title, latestStored.text)
            }
        }

        if (isUnchangedForKey(key, title, text)) return

        val entity = NotificationEntity(
            packageName = pkg,
            appName = resolvedAppName,
            title = title,
            text = text,
            timestamp = postTime,
            isOngoing = isOngoing,
            notificationKey = key,
            sender = null,
            isOutgoing = false,
            isGroup = false,
            removedAt = null,
            removalReason = null
        )

        dao.insert(entity)
    }

    private suspend fun captureMessagingStyleNotification(
        pkg: String,
        resolvedAppName: String,
        key: String,
        isOngoing: Boolean,
        postTime: Long,
        extras: Bundle?,
        messagingStyle: NotificationCompat.MessagingStyle
    ) {
        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
        val styleUser = messagingStyle.user

        val rawConversationTitle = messagingStyle.conversationTitle?.toString()?.trim().orEmpty()
            .ifEmpty {
                extras?.let { safeCharSequenceToString(it, Notification.EXTRA_CONVERSATION_TITLE) }
                    .orEmpty()
            }

        val isGroup = messagingStyle.isGroupConversation || rawConversationTitle.isNotEmpty()

        val conversationTitle = when {
            rawConversationTitle.isNotEmpty() -> rawConversationTitle
            else -> {
                val otherParticipant = messagingStyle.messages
                    .asSequence()
                    .mapNotNull { msg ->
                        val person = msg.person
                        if (!isOutgoingPerson(person, styleUser)) {
                            person?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                        } else {
                            null
                        }
                    }
                    .firstOrNull()

                otherParticipant
                    ?: extras?.let { safeCharSequenceToString(it, Notification.EXTRA_TITLE) }
                        ?.takeIf { it.isNotEmpty() && !it.equals(OUTGOING_SENDER_LABEL, ignoreCase = true) }
                    ?: extractTitle(extras, resolvedAppName)
            }
        }

        val previousLatestTs = getLatestMessageTimestamp(key)
        var maxHandledTs = previousLatestTs ?: Long.MIN_VALUE

        val sortedMessages = messagingStyle.messages.sortedBy { msg ->
            if (msg.timestamp > 0L) msg.timestamp else postTime
        }

        for (msg in sortedMessages) {
            val msgText = msg.text?.toString()?.trim().orEmpty()
            if (msgText.isEmpty()) continue

            val msgTimestamp = if (msg.timestamp > 0L) msg.timestamp else postTime
            if (previousLatestTs != null && msgTimestamp <= previousLatestTs) {
                continue
            }

            val isOutgoing = isOutgoingPerson(msg.person, styleUser)
            val senderName: String? = if (isOutgoing) {
                OUTGOING_SENDER_LABEL
            } else {
                msg.person?.name?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }

            val alreadyExists = dao.existsMessage(
                notificationKey = key,
                sender = senderName,
                text = msgText,
                timestamp = msgTimestamp
            )

            if (!alreadyExists) {
                dao.insert(
                    NotificationEntity(
                        packageName = pkg,
                        appName = resolvedAppName,
                        title = conversationTitle,
                        text = msgText,
                        timestamp = msgTimestamp,
                        isOngoing = isOngoing,
                        notificationKey = key,
                        sender = senderName,
                        isOutgoing = isOutgoing,
                        isGroup = isGroup,
                        removedAt = null,
                        removalReason = null
                    )
                )
            }

            if (msgTimestamp > maxHandledTs) {
                maxHandledTs = msgTimestamp
            }
        }

        if (maxHandledTs > Long.MIN_VALUE) {
            setLatestMessageTimestamp(key, maxHandledTs)
        }
    }

    private fun isOutgoingPerson(messagePerson: Person?, styleUser: Person?): Boolean {
        if (messagePerson == null) return true
        val msgName = messagePerson.name?.toString()?.trim().orEmpty()
        if (msgName.equals(OUTGOING_SENDER_LABEL, ignoreCase = true)) return true

        if (styleUser != null) {
            val msgKey = messagePerson.key?.takeIf { it.isNotBlank() }
            val userKey = styleUser.key?.takeIf { it.isNotBlank() }
            if (msgKey != null && userKey != null && msgKey == userKey) {
                return true
            }

            val msgUri = messagePerson.uri?.takeIf { it.isNotBlank() }
            val userUri = styleUser.uri?.takeIf { it.isNotBlank() }
            if (msgUri != null && userUri != null && msgUri == userUri) {
                return true
            }

            val userName = styleUser.name?.toString()?.trim().orEmpty()
            if (msgName.isNotEmpty() && userName.isNotEmpty() && msgName.equals(userName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun getLatestMessageTimestamp(key: String): Long? {
        synchronized(latestMessageLock) {
            return latestMessageTimestampByKey[key]
        }
    }

    private fun setLatestMessageTimestamp(key: String, timestamp: Long) {
        synchronized(latestMessageLock) {
            val current = latestMessageTimestampByKey[key]
            if (current == null || timestamp >= current) {
                latestMessageTimestampByKey[key] = timestamp
            }
        }
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

    private fun hasTrackedHashForKey(key: String): Boolean {
        synchronized(recentKeyLock) {
            return lastContentHashByKey.containsKey(key)
        }
    }

    private fun seedHashForKey(key: String, title: String, text: String) {
        val contentHash = 31 * title.hashCode() + text.hashCode()
        synchronized(recentKeyLock) {
            if (!lastContentHashByKey.containsKey(key)) {
                lastContentHashByKey[key] = contentHash
            }
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

    private fun resolveNotificationKey(sbn: StatusBarNotification): String {
        val pkg = sbn.packageName.orEmpty()
        return sbn.key?.takeIf { it.isNotBlank() } ?: "${pkg}_${sbn.id}_${sbn.tag.orEmpty()}"
    }

    private fun rebuildActiveContentIntents(activeList: Array<StatusBarNotification>) {
        synchronized(activeIntentsLock) {
            activeContentIntents.clear()
            val sorted = activeList.sortedBy { it.postTime }
            for (sbn in sorted) {
                val pkg = sbn.packageName?.takeIf { it.isNotBlank() } ?: continue
                if (pkg == applicationContext.packageName) continue
                val intent = sbn.notification?.contentIntent ?: continue
                val key = resolveNotificationKey(sbn)
                activeContentIntents[key] = intent
            }
        }
    }

    companion object {
        private const val MAX_TRACKED_KEYS = 100
        private const val FALLBACK_NO_TEXT = "(no text)"
        const val OUTGOING_SENDER_LABEL = "You"

        private val activeIntentsLock = Any()
        private val activeContentIntents =
            object : LinkedHashMap<String, PendingIntent>(MAX_TRACKED_KEYS, 0.75f, true) {
                override fun removeEldestEntry(
                    eldest: MutableMap.MutableEntry<String, PendingIntent>?
                ): Boolean {
                    return size > MAX_TRACKED_KEYS
                }
            }

        fun updateActiveContentIntent(key: String, contentIntent: PendingIntent?) {
            if (key.isBlank()) return
            synchronized(activeIntentsLock) {
                if (contentIntent != null) {
                    activeContentIntents[key] = contentIntent
                } else {
                    activeContentIntents.remove(key)
                }
            }
        }

        fun getActiveContentIntent(key: String): PendingIntent? {
            if (key.isBlank()) return null
            synchronized(activeIntentsLock) {
                return activeContentIntents[key]
            }
        }

        fun removeActiveContentIntent(key: String) {
            if (key.isBlank()) return
            synchronized(activeIntentsLock) {
                activeContentIntents.remove(key)
            }
        }

        fun clearActiveContentIntents() {
            synchronized(activeIntentsLock) {
                activeContentIntents.clear()
            }
        }

        fun mapRemovalReason(reason: Int): String = when (reason) {
            REASON_CLICK -> "Opened"
            REASON_CANCEL -> "Dismissed"
            REASON_CANCEL_ALL -> "Cleared all"
            REASON_APP_CANCEL, REASON_APP_CANCEL_ALL -> "Removed by app"
            REASON_SNOOZED -> "Snoozed"
            else -> "Other"
        }

        fun openNotificationOrApp(
            context: Context,
            notificationKey: String,
            packageName: String
        ): Boolean {
            val activeIntent = getActiveContentIntent(notificationKey)
            if (activeIntent != null) {
                val fired = trySendPendingIntent(context, activeIntent, notificationKey)
                if (fired) return true
            }

            if (packageName.isBlank()) return false
            val launchIntent = try {
                context.packageManager.getLaunchIntentForPackage(packageName)
            } catch (_: Throwable) {
                null
            } ?: return false

            return try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } catch (_: Throwable) {
                false
            }
        }

        private fun trySendPendingIntent(
            context: Context,
            pendingIntent: PendingIntent,
            notificationKey: String
        ): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }
                    pendingIntent.send(
                        context,
                        0,
                        null,
                        null,
                        null,
                        null,
                        options.toBundle()
                    )
                } else {
                    pendingIntent.send()
                }
                true
            } catch (_: PendingIntent.CanceledException) {
                removeActiveContentIntent(notificationKey)
                false
            } catch (_: Throwable) {
                try {
                    pendingIntent.send()
                    true
                } catch (_: PendingIntent.CanceledException) {
                    removeActiveContentIntent(notificationKey)
                    false
                } catch (_: Throwable) {
                    false
                }
            }
        }
    }
}
