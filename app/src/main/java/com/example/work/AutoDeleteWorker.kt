package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AutoDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settingsRepository = SettingsRepository(applicationContext)
            val settings = settingsRepository.settingsFlow.first()
            val retentionDays = settings.autoDeleteOption.days
            if (settings.autoDeleteEnabled && retentionDays != null && retentionDays > 0) {
                val cutoff = System.currentTimeMillis() - retentionDays * MILLIS_PER_DAY
                AppDatabase.getInstance(applicationContext)
                    .notificationDao()
                    .deleteOlderThan(cutoff)
            }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
