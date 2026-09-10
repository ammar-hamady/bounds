package com.example.bounds.util

import com.example.bounds.model.WebsiteEnforcementStatus
import java.util.concurrent.atomic.AtomicLong

enum class WebsiteConsentRecoveryAction {
    RETRY_ACTIVE_POLICY,
    MARK_READY,
    MARK_CONSENT_REQUIRED
}

data class WebsiteConsentRecovery(
    val action: WebsiteConsentRecoveryAction,
    val approvalWasDenied: Boolean = false
)

/**
 * Pure decision used after Android's VPN consent activity returns.
 */
fun resolveWebsiteConsentRecovery(
    hasVpnConsent: Boolean,
    approvalReported: Boolean,
    hasEligibleActivePolicy: Boolean
): WebsiteConsentRecovery {
    if (!hasVpnConsent) {
        return WebsiteConsentRecovery(
            action = WebsiteConsentRecoveryAction.MARK_CONSENT_REQUIRED,
            approvalWasDenied = !approvalReported
        )
    }
    return WebsiteConsentRecovery(
        action = if (hasEligibleActivePolicy) {
            WebsiteConsentRecoveryAction.RETRY_ACTIVE_POLICY
        } else {
            WebsiteConsentRecoveryAction.MARK_READY
        }
    )
}

fun shouldPreserveWebsiteStatusDuringPassiveRefresh(
    status: WebsiteEnforcementStatus
): Boolean = status == WebsiteEnforcementStatus.AWAITING_CONSENT ||
    status == WebsiteEnforcementStatus.STARTING ||
    status == WebsiteEnforcementStatus.ERROR

class WebsiteStartupAttemptTracker {
    private val generation = AtomicLong(0)

    fun begin(): Long = generation.incrementAndGet()

    fun invalidate() {
        generation.incrementAndGet()
    }

    fun isCurrent(attempt: Long): Boolean = generation.get() == attempt
}