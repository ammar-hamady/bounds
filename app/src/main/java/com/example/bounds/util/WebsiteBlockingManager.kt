package com.example.bounds.util

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.bounds.BoundsApplication
import com.example.bounds.model.WebsiteEnforcementState
import com.example.bounds.model.WebsiteEnforcementStatus
import com.example.bounds.service.BoundsVpnService

/**
 * Small coordination boundary between zone enforcement, the settings screen,
 * and Android's single-active-VPN system policy.
 */
object WebsiteBlockingManager {

    private const val STARTUP_TIMEOUT_MS = 6_000L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val startupAttempts = WebsiteStartupAttemptTracker()

    fun refreshReadiness(context: Context, preserveReportedState: Boolean = true) {
        val app = context.applicationContext as BoundsApplication
        if (
            preserveReportedState &&
            (
                app.websiteEnforcement.value.status == WebsiteEnforcementStatus.DISPLACED ||
                    shouldPreserveWebsiteStatusDuringPassiveRefresh(
                        app.websiteEnforcement.value.status
                    )
                )
        ) {
            return
        }
        if (VpnService.prepare(context) == null) {
            if (app.websiteEnforcement.value.status != WebsiteEnforcementStatus.ACTIVE) {
                app.setWebsiteEnforcement(
                    WebsiteEnforcementState(
                        status = WebsiteEnforcementStatus.READY,
                        message = "VPN consent is ready. Website rules activate inside a zone."
                    )
                )
            }
        } else {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.CONSENT_REQUIRED,
                    message = "VPN consent is required before Bounds can block websites."
                )
            )
        }
    }

    fun markConsentPending(context: Context) {
        (context.applicationContext as BoundsApplication).setWebsiteEnforcement(
            WebsiteEnforcementState(
                status = WebsiteEnforcementStatus.AWAITING_CONSENT,
                message = "Waiting for Android VPN approval."
            )
        )
    }

    fun handleConsentResult(context: Context, approvalReported: Boolean) {
        val app = context.applicationContext as BoundsApplication
        val active = app.activeEnforcement.value
        val hasEligibleActivePolicy =
            active != null && !active.isGracePeriod && active.blockedDomains.isNotEmpty()
        val recovery = resolveWebsiteConsentRecovery(
            hasVpnConsent = VpnService.prepare(context) == null,
            approvalReported = approvalReported,
            hasEligibleActivePolicy = hasEligibleActivePolicy
        )

        when (recovery.action) {
            WebsiteConsentRecoveryAction.RETRY_ACTIVE_POLICY -> retryActivePolicy(context)
            WebsiteConsentRecoveryAction.MARK_READY -> markReady(app)
            WebsiteConsentRecoveryAction.MARK_CONSENT_REQUIRED -> {
                app.setWebsiteEnforcement(
                    WebsiteEnforcementState(
                        status = WebsiteEnforcementStatus.CONSENT_REQUIRED,
                        message = if (recovery.approvalWasDenied) {
                            "VPN approval was cancelled. Approve Bounds to enable website blocking."
                        } else {
                            "Android did not retain VPN approval. Check for an always-on or restricted VPN, then try again."
                        }
                    )
                )
            }
        }
    }

    fun retryActivePolicy(context: Context) {
        val app = context.applicationContext as BoundsApplication
        val active = app.activeEnforcement.value
        if (active == null) {
            refreshReadiness(context, preserveReportedState = false)
            return
        }
        if (!active.isGracePeriod && active.blockedDomains.isNotEmpty()) {
            activate(context, active.zoneId, active.blockedDomains)
        } else {
            refreshReadiness(context)
        }
    }

    /**
     * Starts or replaces the local DNS filter. This is deliberately a no-op
     * with a truthful unavailable state when consent has not been granted;
     * background geofence callbacks must never launch a consent activity.
     */
    fun activate(context: Context, zoneId: String, domains: List<String>): Boolean {
        val app = context.applicationContext as BoundsApplication
        val canonicalDomains = DomainBlocklist.canonicalizeAll(domains)
        if (canonicalDomains.isEmpty()) {
            deactivate(context, zoneId)
            return false
        }
        if (VpnService.prepare(context) != null) {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.CONSENT_REQUIRED,
                    message = "Approve VPN access in Settings to block this zone's websites.",
                    activeZoneId = zoneId,
                    domains = canonicalDomains
                )
            )
            return false
        }

        val startupAttempt = startupAttempts.begin()
        app.setWebsiteEnforcement(
            WebsiteEnforcementState(
                status = WebsiteEnforcementStatus.STARTING,
                message = "Starting website protection for this zone…",
                activeZoneId = zoneId,
                domains = canonicalDomains
            )
        )
        val intent = Intent(context, BoundsVpnService::class.java).apply {
            action = BoundsVpnService.ACTION_START
            putExtra(BoundsVpnService.EXTRA_ZONE_ID, zoneId)
            putStringArrayListExtra(
                BoundsVpnService.EXTRA_BLOCKED_DOMAINS,
                ArrayList(canonicalDomains)
            )
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            mainHandler.postDelayed(
                {
                    val current = app.websiteEnforcement.value
                    if (
                        startupAttempts.isCurrent(startupAttempt) &&
                        current.status == WebsiteEnforcementStatus.STARTING &&
                        current.activeZoneId == zoneId
                    ) {
                        app.setWebsiteEnforcement(
                            current.copy(
                                status = WebsiteEnforcementStatus.ERROR,
                                message = "Bounds VPN did not finish starting. Try again from Settings."
                            )
                        )
                    }
                },
                STARTUP_TIMEOUT_MS
            )
            return true
        } catch (e: Exception) {
            startupAttempts.invalidate()
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.ERROR,
                    message = "Bounds could not start its VPN service. Try again from Settings.",
                    activeZoneId = zoneId,
                    domains = canonicalDomains
                )
            )
            return false
        }
    }

    fun deactivate(context: Context, zoneId: String? = null) {
        val app = context.applicationContext as BoundsApplication
        val current = app.websiteEnforcement.value
        if (zoneId != null && current.activeZoneId != null && current.activeZoneId != zoneId) {
            return
        }
        startupAttempts.invalidate()
        context.stopService(Intent(context, BoundsVpnService::class.java))
        if (VpnService.prepare(context) == null) {
            markReady(app)
        } else {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.CONSENT_REQUIRED,
                    message = "VPN consent is required before Bounds can block websites."
                )
            )
        }
    }

    private fun markReady(app: BoundsApplication) {
        app.setWebsiteEnforcement(
            WebsiteEnforcementState(
                status = WebsiteEnforcementStatus.READY,
                message = "VPN consent is ready. Website rules activate inside a zone."
            )
        )
    }
}
