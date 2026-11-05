package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AutoDeleteOption
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeOption
import com.example.data.repository.UserSettings
import com.example.work.AutoDeleteScheduler
import com.example.work.AutoDeleteWorker
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val settingsState: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    val savedNotificationsCount: StateFlow<Int> = notificationRepository.totalSavedCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _allClearedChannel = Channel<Unit>(Channel.CONFLATED)
    val allClearedEvents: Flow<Unit> = _allClearedChannel.receiveAsFlow()

    fun updateTheme(option: ThemeOption) {
        viewModelScope.launch {
            settingsRepository.setThemeOption(option)
        }
    }

    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun updateHideOngoingAndSystem(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHideOngoingAndSystem(enabled)
        }
    }

    fun updateAutoDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val resolvedOption = settingsRepository.setAutoDeleteEnabled(enabled)
            AutoDeleteScheduler.updatePeriodicSchedule(
                context = appContext,
                option = resolvedOption,
                rescheduleOnValueChange = true
            )
            val days = resolvedOption.days
            if (enabled && days != null && days > 0) {
                val cutoff = System.currentTimeMillis() - days * AutoDeleteWorker.MILLIS_PER_DAY
                notificationRepository.deleteOlderThan(cutoff)
            }
        }
    }

    fun updateAutoDeleteOption(option: AutoDeleteOption) {
        viewModelScope.launch {
            settingsRepository.setAutoDeleteOption(option)
            AutoDeleteScheduler.updatePeriodicSchedule(
                context = appContext,
                option = option,
                rescheduleOnValueChange = true
            )
            val days = option.days
            if (days != null && days > 0) {
                val cutoff = System.currentTimeMillis() - days * AutoDeleteWorker.MILLIS_PER_DAY
                notificationRepository.deleteOlderThan(cutoff)
            }
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.deleteAll()
            _allClearedChannel.trySend(Unit)
        }
    }

    class Factory(
        private val appContext: Context,
        private val settingsRepository: SettingsRepository,
        private val notificationRepository: NotificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                appContext.applicationContext,
                settingsRepository,
                notificationRepository
            ) as T
        }
    }
}
