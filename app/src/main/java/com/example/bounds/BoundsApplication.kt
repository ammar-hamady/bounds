package com.example.bounds

import android.app.Application
import com.example.bounds.data.AnalyticsRepository
import com.example.bounds.data.ActiveEnforcementMutationGuard
import com.example.bounds.data.SettingsRepository
import com.example.bounds.data.ZoneRepository
import com.example.bounds.model.ActiveEnforcementInfo
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.model.BlockIntensity
import com.example.bounds.model.Zone
import com.example.bounds.model.WebsiteEnforcementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Application-level singleton that bridges the geofence BroadcastReceiver / Service
 * (which have no access to Compose state) with the UI layer via StateFlow.
 */
class BoundsApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settingsReady = CompletableDeferred<Unit>()
    private val enforcementMutationGuard = ActiveEnforcementMutationGuard()

    // ── Repositories (persistent storage) ────────────────────────────────────
    lateinit var zoneRepository: ZoneRepository
        private set
    lateinit var analyticsRepository: AnalyticsRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    // ── Active zone enforcement ───────────────────────────────────────────────
    private val _activeEnforcement = MutableStateFlow<ActiveEnforcementInfo?>(null)
    val activeEnforcement: StateFlow<ActiveEnforcementInfo?> = _activeEnforcement.asStateFlow()

    private val _websiteEnforcement = MutableStateFlow(WebsiteEnforcementState())
    val websiteEnforcement: StateFlow<WebsiteEnforcementState> = _websiteEnforcement.asStateFlow()

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

    /** Whether zone-entry alerts are enabled. */
    @Volatile var entryNotificationsEnabled: Boolean = false

    /** Strict mode removes the temporary bypass action from blocking overlays. */
    @Volatile var blockIntensity: BlockIntensity = BlockIntensity.STRICT

    override fun onCreate() {
        super.onCreate()
        zoneRepository = ZoneRepository(this, enforcementMutationGuard)
        analyticsRepository = AnalyticsRepository(this)
        settingsRepository = SettingsRepository(this, enforcementMutationGuard)

        // Services and boot receivers can run before MainActivity is opened, so
        // keep service-facing settings synchronized directly from DataStore.
        applicationScope.launch {
            combine(
                settingsRepository.graceTimerSecondsFlow,
                settingsRepository.hapticFeedbackEnabledFlow,
                settingsRepository.entryNotificationsEnabledFlow,
                settingsRepository.blockIntensityFlow
            ) { grace, haptics, entryNotifications, intensity ->
                RuntimeSettings(grace, haptics, entryNotifications, intensity)
            }.collect { settings ->
                graceTimerSeconds = settings.graceSeconds
                hapticFeedbackEnabled = settings.haptics
                entryNotificationsEnabled = settings.entryNotifications
                blockIntensity = settings.intensity
                if (!settingsReady.isCompleted) settingsReady.complete(Unit)
            }
        }
    }

    suspend fun awaitSettingsReady() {
        settingsReady.await()
    }

    fun setEnforcement(info: ActiveEnforcementInfo?) {
        enforcementMutationGuard.updateProtectedZone(info?.zoneId) {
            _activeEnforcement.value = info
        }
    }
    fun setWebsiteEnforcement(state: WebsiteEnforcementState) {
        _websiteEnforcement.value = state
    }
    fun postAnalyticsEvent(event: AnalyticsEvent) { _pendingAnalytics.value = event }
    fun consumeAnalyticsEvent() { _pendingAnalytics.value = null }

    private data class RuntimeSettings(
        val graceSeconds: Int,
        val haptics: Boolean,
        val entryNotifications: Boolean,
        val intensity: BlockIntensity
    )
}
