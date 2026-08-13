package com.example.bounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bounds.data.AnalyticsRepository
import com.example.bounds.data.SettingsRepository
import com.example.bounds.data.ZoneRepository
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.ThemePreference
import com.example.bounds.model.Zone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoundsViewModel(
    private val zoneRepository: ZoneRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val zones: StateFlow<List<Zone>> = zoneRepository.zonesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val analyticsEvents: StateFlow<List<AnalyticsEvent>> = analyticsRepository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Settings ──────────────────────────────────────────────────────────────
    val themePreference: StateFlow<ThemePreference> = settingsRepository.themePreferenceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.DARK)

    val graceTimerSeconds: StateFlow<Int> = settingsRepository.graceTimerSecondsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun saveZones(zones: List<Zone>) {
        viewModelScope.launch { zoneRepository.saveZones(zones) }
    }

    fun addEvent(event: AnalyticsEvent) {
        viewModelScope.launch {
            analyticsRepository.saveEvents(analyticsEvents.value + event)
        }
    }

    fun clearEvents() {
        viewModelScope.launch { analyticsRepository.saveEvents(emptyList()) }
    }

    fun saveThemePreference(pref: ThemePreference) {
        viewModelScope.launch { settingsRepository.saveThemePreference(pref) }
    }

    fun saveGraceTimerSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.saveGraceTimerSeconds(seconds) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as BoundsApplication
                BoundsViewModel(app.zoneRepository, app.analyticsRepository, app.settingsRepository)
            }
        }
    }
}
