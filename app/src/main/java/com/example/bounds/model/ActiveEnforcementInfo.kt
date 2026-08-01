package com.example.bounds.model

/**
 * Snapshot of an actively-enforced geofence zone.
 * Stored in BoundsApplication.activeEnforcement and collected by the UI.
 */
data class ActiveEnforcementInfo(
    val zoneId: String,
    val zoneName: String,
    /** Friendly app names e.g. "Instagram", "TikTok" */
    val blockedApps: List<String>,
    /** True while the grace-period countdown is still running (not yet blocking). */
    val isGracePeriod: Boolean = false
)
