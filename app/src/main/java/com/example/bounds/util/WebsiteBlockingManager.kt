package com.example.bounds.util

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
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

    fun refreshReadiness(context: Context) {
        val app = context.applicationContext as BoundsApplication
        if (app.websiteEnforcement.value.status == WebsiteEnforcementStatus.DISPLACED) {
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
                    status = WebsiteEnforcementStatus.UNAVAILABLE,
                    message = "VPN consent is required before Bounds can block websites."
                )
            )
        }
    }

    fun retryActivePolicy(context: Context) {
        val app = context.applicationContext as BoundsApplication
        val active = app.activeEnforcement.value ?: return
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
                    status = WebsiteEnforcementStatus.UNAVAILABLE,
                    message = "Approve VPN access in Settings to block this zone's websites.",
                    activeZoneId = zoneId,
                    domains = canonicalDomains
                )
            )
            return false
        }

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
            return true
        } catch (e: Exception) {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.UNAVAILABLE,
                    message = "Bounds could not start website protection. Try again after reopening the app.",
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
        context.stopService(Intent(context, BoundsVpnService::class.java))
        if (VpnService.prepare(context) == null) {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.READY,
                    message = "VPN consent is ready. Website rules activate inside a zone."
                )
            )
        } else {
            app.setWebsiteEnforcement(
                WebsiteEnforcementState(
                    status = WebsiteEnforcementStatus.UNAVAILABLE,
                    message = "VPN consent is required before Bounds can block websites."
                )
            )
        }
    }
}