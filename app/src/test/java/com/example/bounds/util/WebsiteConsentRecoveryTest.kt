package com.example.bounds.util

import com.example.bounds.model.WebsiteEnforcementStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteConsentRecoveryTest {

    @Test
    fun `approved consent retries an eligible active policy`() {
        val recovery = resolveWebsiteConsentRecovery(
            hasVpnConsent = true,
            approvalReported = true,
            hasEligibleActivePolicy = true
        )

        assertEquals(
            WebsiteConsentRecoveryAction.RETRY_ACTIVE_POLICY,
            recovery.action
        )
        assertFalse(recovery.approvalWasDenied)
    }

    @Test
    fun `approved consent without active policy becomes ready`() {
        val recovery = resolveWebsiteConsentRecovery(
            hasVpnConsent = true,
            approvalReported = true,
            hasEligibleActivePolicy = false
        )

        assertEquals(WebsiteConsentRecoveryAction.MARK_READY, recovery.action)
    }

    @Test
    fun `cancelled consent remains actionable`() {
        val recovery = resolveWebsiteConsentRecovery(
            hasVpnConsent = false,
            approvalReported = false,
            hasEligibleActivePolicy = true
        )

        assertEquals(
            WebsiteConsentRecoveryAction.MARK_CONSENT_REQUIRED,
            recovery.action
        )
        assertTrue(recovery.approvalWasDenied)
    }

    @Test
    fun `reported approval not retained remains consent required`() {
        val recovery = resolveWebsiteConsentRecovery(
            hasVpnConsent = false,
            approvalReported = true,
            hasEligibleActivePolicy = false
        )

        assertEquals(
            WebsiteConsentRecoveryAction.MARK_CONSENT_REQUIRED,
            recovery.action
        )
        assertFalse(recovery.approvalWasDenied)
    }

    @Test
    fun `passive resume refresh preserves pending and runtime failures`() {
        assertTrue(
            shouldPreserveWebsiteStatusDuringPassiveRefresh(
                WebsiteEnforcementStatus.AWAITING_CONSENT
            )
        )
        assertTrue(
            shouldPreserveWebsiteStatusDuringPassiveRefresh(
                WebsiteEnforcementStatus.STARTING
            )
        )
        assertTrue(
            shouldPreserveWebsiteStatusDuringPassiveRefresh(
                WebsiteEnforcementStatus.ERROR
            )
        )
        assertFalse(
            shouldPreserveWebsiteStatusDuringPassiveRefresh(
                WebsiteEnforcementStatus.READY
            )
        )
    }

    @Test
    fun `retry invalidates the previous startup timeout`() {
        val tracker = WebsiteStartupAttemptTracker()
        val firstAttempt = tracker.begin()
        val retryAttempt = tracker.begin()

        assertFalse(tracker.isCurrent(firstAttempt))
        assertTrue(tracker.isCurrent(retryAttempt))

        tracker.invalidate()
        assertFalse(tracker.isCurrent(retryAttempt))
    }
}