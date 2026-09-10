package com.example.bounds.model

/**
 * Truthful state exposed by the website filter. A domain policy is never
 * described as active unless the VPN service successfully established its TUN.
 */
enum class WebsiteEnforcementStatus {
    UNAVAILABLE,
    READY,
    ACTIVE,
    DISPLACED
}

data class WebsiteEnforcementState(
    val status: WebsiteEnforcementStatus = WebsiteEnforcementStatus.UNAVAILABLE,
    val message: String = "VPN approval is required for website blocking.",
    val activeZoneId: String? = null,
    val domains: List<String> = emptyList()
)