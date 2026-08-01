package com.example.bounds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bounds.data.AnalyticsRepository
import com.example.bounds.data.ZoneRepository
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.Zone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BoundsViewModel(
    private val zoneRepository: ZoneRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val zones: StateFlow<List<Zone>> = zoneRepository.zonesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val analyticsEvents: StateFlow<List<AnalyticsEvent>> = analyticsRepository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as BoundsApplication
                BoundsViewModel(app.zoneRepository, app.analyticsRepository)
            }
        }
    }
}
