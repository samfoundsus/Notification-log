package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_log_settings"
)

enum class ThemeOption(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeOption =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

enum class AutoDeleteOption(val storageKey: String, val days: Int?) {
    OFF("off", null),
    DAYS_7("7_days", 7),
    DAYS_30("30_days", 30),
    DAYS_90("90_days", 90);

    companion object {
        fun fromKey(key: String?): AutoDeleteOption =
            entries.firstOrNull { it.storageKey == key } ?: DAYS_30
    }
}

data class UserSettings(
    val themeOption: ThemeOption = ThemeOption.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val autoDeleteEnabled: Boolean = false,
    val autoDeleteOption: AutoDeleteOption = AutoDeleteOption.OFF
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_option")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val AUTO_DELETE_ENABLED = booleanPreferencesKey("auto_delete_enabled")
        val AUTO_DELETE_OPTION = stringPreferencesKey("auto_delete_option")
        val LAST_NON_OFF_AUTO_DELETE = stringPreferencesKey("last_non_off_auto_delete")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        val theme = ThemeOption.fromKey(prefs[Keys.THEME])
        val dynamicColor = prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true
        val rawAutoDelete = prefs[Keys.AUTO_DELETE_OPTION]
        val autoDeleteOption = if (rawAutoDelete == null) {
            AutoDeleteOption.OFF
        } else {
            AutoDeleteOption.fromKey(rawAutoDelete)
        }
        val enabled = prefs[Keys.AUTO_DELETE_ENABLED] ?: (autoDeleteOption != AutoDeleteOption.OFF)
        UserSettings(
            themeOption = theme,
            dynamicColorEnabled = dynamicColor,
            autoDeleteEnabled = enabled,
            autoDeleteOption = if (enabled && autoDeleteOption == AutoDeleteOption.OFF) {
                AutoDeleteOption.DAYS_30
            } else if (!enabled) {
                AutoDeleteOption.OFF
            } else {
                autoDeleteOption
            }
        )
    }

    suspend fun setThemeOption(option: ThemeOption) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.THEME] = option.storageKey
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setAutoDeleteEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_DELETE_ENABLED] = enabled
            if (enabled) {
                val restored = AutoDeleteOption.fromKey(prefs[Keys.LAST_NON_OFF_AUTO_DELETE])
                val target = if (restored == AutoDeleteOption.OFF) AutoDeleteOption.DAYS_30 else restored
                prefs[Keys.AUTO_DELETE_OPTION] = target.storageKey
            } else {
                prefs[Keys.AUTO_DELETE_OPTION] = AutoDeleteOption.OFF.storageKey
            }
        }
    }

    suspend fun setAutoDeleteOption(option: AutoDeleteOption) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_DELETE_OPTION] = option.storageKey
            if (option == AutoDeleteOption.OFF) {
                prefs[Keys.AUTO_DELETE_ENABLED] = false
            } else {
                prefs[Keys.AUTO_DELETE_ENABLED] = true
                prefs[Keys.LAST_NON_OFF_AUTO_DELETE] = option.storageKey
            }
        }
    }
}
