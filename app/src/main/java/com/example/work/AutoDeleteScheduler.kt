package com.example.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.repository.AutoDeleteOption
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object AutoDeleteScheduler {

    const val UNIQUE_WORK_NAME = "notification_log_auto_delete_daily"

    fun updatePeriodicSchedule(
        context: Context,
        option: AutoDeleteOption,
        rescheduleOnValueChange: Boolean
    ) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val days = option.days
        if (option == AutoDeleteOption.OFF || days == null || days <= 0) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        if (rescheduleOnValueChange) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        val request = PeriodicWorkRequestBuilder<AutoDeleteWorker>(1, TimeUnit.DAYS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    suspend fun runStartupCleanupAndSchedule(
        context: Context,
        notificationRepository: NotificationRepository,
        settingsRepository: SettingsRepository
    ) = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.settingsFlow.first()
            val option = settings.autoDeleteOption
            val days = option.days
            if (settings.autoDeleteEnabled && days != null && days > 0) {
                val cutoff = System.currentTimeMillis() - days * AutoDeleteWorker.MILLIS_PER_DAY
                notificationRepository.deleteOlderThan(cutoff)
            }
            updatePeriodicSchedule(
                context = context,
                option = if (settings.autoDeleteEnabled) option else AutoDeleteOption.OFF,
                rescheduleOnValueChange = false
            )
        } catch (_: Throwable) {
            // Never fail app startup if WorkManager or cleanup encounters an exception
        }
    }
}
