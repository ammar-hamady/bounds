package com.example.bounds.model

/**
 * Truthful state exposed by the website filter. A domain policy is never
 * described as active unless the VPN service successfully established its TUN.
 */
enum class WebsiteEnforcementStatus {
    CONSENT_REQUIRED,
    AWAITING_CONSENT,
    READY,
    STARTING,
    ACTIVE,
    DISPLACED,
    ERROR
}

data class WebsiteEnforcementState(
    val status: WebsiteEnforcementStatus = WebsiteEnforcementStatus.CONSENT_REQUIRED,
    val message: String = "VPN approval is required for website blocking.",
    val activeZoneId: String? = null,
    val domains: List<String> = emptyList()
)