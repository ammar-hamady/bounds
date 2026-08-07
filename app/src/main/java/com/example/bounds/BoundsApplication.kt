package com.example.bounds

import android.app.Application
import com.example.bounds.data.AnalyticsRepository
import com.example.bounds.data.ZoneRepository
import com.example.bounds.model.ActiveEnforcementInfo
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.Zone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Application-level singleton that bridges the geofence BroadcastReceiver / Service
 * (which have no access to Compose state) with the UI layer via StateFlow.
 */
class BoundsApplication : Application() {

    // ── Repositories (persistent storage) ────────────────────────────────────
    lateinit var zoneRepository: ZoneRepository
        private set
    lateinit var analyticsRepository: AnalyticsRepository
        private set

    // ── Active zone enforcement ───────────────────────────────────────────────
    private val _activeEnforcement = MutableStateFlow<ActiveEnforcementInfo?>(null)
    val activeEnforcement: StateFlow<ActiveEnforcementInfo?> = _activeEnforcement.asStateFlow()

    // ── Analytics events produced by zone sessions ────────────────────────────
    private val _pendingAnalytics = MutableStateFlow<AnalyticsEvent?>(null)
    val pendingAnalytics: StateFlow<AnalyticsEvent?> = _pendingAnalytics.asStateFlow()

    /**
     * In-memory zone list kept in sync from the ViewModel so the
     * GeofenceBroadcastReceiver can resolve geofence IDs → zone data.
     */
    @Volatile var zones: List<Zone> = emptyList()

    /** Grace-timer preference kept in sync from Settings. */
    @Volatile var graceTimerSeconds: Int = 0

    /** Haptic feedback toggle kept in sync from Settings. */
    @Volatile var hapticFeedbackEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        zoneRepository = ZoneRepository(this)
        analyticsRepository = AnalyticsRepository(this)
    }

    fun setEnforcement(info: ActiveEnforcementInfo?) { _activeEnforcement.value = info }
    fun postAnalyticsEvent(event: AnalyticsEvent) { _pendingAnalytics.value = event }
    fun consumeAnalyticsEvent() { _pendingAnalytics.value = null }
}
