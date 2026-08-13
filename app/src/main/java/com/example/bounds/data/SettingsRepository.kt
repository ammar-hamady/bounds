package com.example.bounds.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bounds.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val themeKey       = stringPreferencesKey("theme_preference")
    private val graceTimerKey  = intPreferencesKey("grace_timer_seconds")

    val themePreferenceFlow: Flow<ThemePreference> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[themeKey] ?: ThemePreference.DARK.name
        runCatching { ThemePreference.valueOf(name) }.getOrDefault(ThemePreference.DARK)
    }

    val graceTimerSecondsFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[graceTimerKey] ?: 0
    }

    suspend fun saveThemePreference(pref: ThemePreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeKey] = pref.name
        }
    }

    suspend fun saveGraceTimerSeconds(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[graceTimerKey] = seconds
        }
    }
}
