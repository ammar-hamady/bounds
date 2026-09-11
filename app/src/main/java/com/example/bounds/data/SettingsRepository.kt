package com.example.bounds.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bounds.model.BlockIntensity
import com.example.bounds.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
    private val mutationGuard: ActiveEnforcementMutationGuard =
        ActiveEnforcementMutationGuard()
) {

    private val themeKey       = stringPreferencesKey("theme_preference")
    private val graceTimerKey  = intPreferencesKey("grace_timer_seconds")
    private val hapticsKey     = booleanPreferencesKey("haptic_feedback_enabled")
    private val entryNotificationsKey = booleanPreferencesKey("entry_notifications_enabled")
    private val blockIntensityKey = stringPreferencesKey("block_intensity")
    private val defaultBlockedAppsKey = stringSetPreferencesKey("default_blocked_apps")

    val themePreferenceFlow: Flow<ThemePreference> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[themeKey] ?: ThemePreference.DARK.name
        runCatching { ThemePreference.valueOf(name) }.getOrDefault(ThemePreference.DARK)
    }

    val graceTimerSecondsFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[graceTimerKey] ?: 0
    }

    val hapticFeedbackEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[hapticsKey] ?: true
    }

    val entryNotificationsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[entryNotificationsKey] ?: false
    }

    val blockIntensityFlow: Flow<BlockIntensity> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[blockIntensityKey] ?: BlockIntensity.STRICT.name
        runCatching { BlockIntensity.valueOf(name) }.getOrDefault(BlockIntensity.STRICT)
    }

    val defaultBlockedAppsFlow: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[defaultBlockedAppsKey]?.toList() ?: emptyList()
    }

    suspend fun saveThemePreference(pref: ThemePreference) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeKey] = pref.name
        }
    }

    suspend fun saveGraceTimerSeconds(seconds: Int): Boolean {
        var persisted = false
        context.settingsDataStore.edit { prefs ->
            persisted = mutationGuard.commitEnforcementSettingMutation {
                prefs[graceTimerKey] = seconds
            }
        }
        return persisted
    }

    suspend fun saveHapticFeedbackEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[hapticsKey] = enabled }
    }

    suspend fun saveEntryNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[entryNotificationsKey] = enabled }
    }

    suspend fun saveBlockIntensity(intensity: BlockIntensity): Boolean {
        var persisted = false
        context.settingsDataStore.edit {
            persisted = mutationGuard.commitEnforcementSettingMutation {
                it[blockIntensityKey] = intensity.name
            }
        }
        return persisted
    }

    suspend fun saveDefaultBlockedApps(apps: List<String>): Boolean {
        var persisted = false
        context.settingsDataStore.edit {
            persisted = mutationGuard.commitEnforcementSettingMutation {
                it[defaultBlockedAppsKey] = apps.toSet()
            }
        }
        return persisted
    }
}
