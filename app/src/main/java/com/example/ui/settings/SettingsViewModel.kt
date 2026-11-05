package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AutoDeleteOption
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeOption
import com.example.data.repository.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settingsState: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

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

    fun updateAutoDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoDeleteEnabled(enabled)
        }
    }

    fun updateAutoDeleteOption(option: AutoDeleteOption) {
        viewModelScope.launch {
            settingsRepository.setAutoDeleteOption(option)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository) as T
        }
    }
}
