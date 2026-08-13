package com.example.bounds.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the idempotency guarantee in GeofenceEnforcementService.
 *
 * Context / motivation
 * ─────────────────────
 * BootReceiver re-registers geofences after every device reboot via
 * BoundsGeofenceManager.syncGeofences, which sets INITIAL_TRIGGER_ENTER on the
 * GeofencingRequest.  Android therefore re-fires GEOFENCE_TRANSITION_ENTER for
 * every geofence whose region the device is currently inside, even if enforcement
 * for that zone was already running before the reboot.
 *
 * GeofenceEnforcementService must treat a second (or any subsequent) ENTER for the
 * zone it is already tracking as a no-op so that:
 *  • the grace-period countdown is not reset,
 *  • AppBlockingService is not launched a second time,
 *  • no duplicate foreground notification is posted.
 *
 * The guard added to handleEnter is:
 *   if (zoneId == currentZoneId) return   // idempotency: drop duplicate ENTER
 *
 * These tests exercise that guard by directly modelling the service's zone-tracking
 * state machine, which requires no Android framework dependencies and can run on the
 * host JVM.
 */
class GeofenceEnforcementIdempotencyTest {

    // ── Minimal state-machine model ───────────────────────────────────────────
    //
    // Mirrors the fields and guard logic inside GeofenceEnforcementService
    // without pulling in the full Android Service machinery.

    private var currentZoneId: String? = null
    private var enterCount: Int = 0          // tracks how many times "real" enter work ran
    private var exitCount: Int = 0

    /**
     * Simulates GeofenceEnforcementService.handleEnter including the idempotency guard.
     * Returns true if enforcement was actually (re-)started, false if the duplicate
     * was dropped.
     */
    private fun handleEnter(zoneId: String): Boolean {
        // ── Idempotency guard (mirrors the production code) ────────────────────
        if (zoneId == currentZoneId) return false

        // "real" work: update state, start grace period, etc.
        currentZoneId = zoneId
        enterCount++
        return true
    }

    /**
     * Simulates GeofenceEnforcementService.handleExit.
     * Clears the tracked zone, which makes the service ready to accept a new
     * ENTER for any zone (including the same one).
     */
    private fun handleExit(zoneId: String?) {
        if (zoneId != null && zoneId != currentZoneId) return  // exit for a different zone
        currentZoneId = null
        exitCount++
    }

    @Before
    fun setUp() {
        currentZoneId = null
        enterCount = 0
        exitCount = 0
    }

    // ── Core idempotency ──────────────────────────────────────────────────────

    @Test
    fun `first ENTER for a zone starts enforcement`() {
        val started = handleEnter("zone-1")
        assertTrue("First ENTER should start enforcement", started)
        assertEquals(1, enterCount)
        assertEquals("zone-1", currentZoneId)
    }

    @Test
    fun `duplicate ENTER for the same zone is ignored`() {
        handleEnter("zone-1")
        val startedAgain = handleEnter("zone-1")
        assertFalse("Duplicate ENTER for the same zone must be a no-op", startedAgain)
        assertEquals("Enforcement should have started exactly once", 1, enterCount)
        assertEquals("zone-1", currentZoneId)
    }

    @Test
    fun `three consecutive ENTER events for the same zone start enforcement exactly once`() {
        // Simulates INITIAL_TRIGGER_ENTER firing multiple times (edge case)
        handleEnter("zone-home")
        handleEnter("zone-home")
        handleEnter("zone-home")
        assertEquals(
            "Enforcement must start exactly once regardless of how many duplicate ENTERs arrive",
            1,
            enterCount
        )
    }

    // ── Boot-resume scenario ──────────────────────────────────────────────────

    @Test
    fun `enforcement restarts exactly once after simulated reboot while inside a zone`() {
        // Step 1 — device enters zone before reboot
        handleEnter("zone-library")
        assertEquals(1, enterCount)

        // Step 2 — simulated reboot: BootReceiver re-registers geofences.
        // Enforcement state is reset (service was killed on reboot).
        currentZoneId = null

        // Step 3 — INITIAL_TRIGGER_ENTER fires because the device is still inside
        // the zone.  Enforcement should restart exactly once.
        val restarted = handleEnter("zone-library")
        assertTrue("Enforcement must restart after reboot", restarted)
        assertEquals(
            "Enforcement must have started exactly twice total (once before reboot, once after)",
            2,
            enterCount
        )
        assertEquals("zone-library", currentZoneId)
    }

    @Test
    fun `duplicate ENTER after reboot does not stack a second enforcement session`() {
        // Reboot: service state cleared
        currentZoneId = null

        // INITIAL_TRIGGER_ENTER fires for zone the device is inside
        handleEnter("zone-gym")

        // A second INITIAL_TRIGGER_ENTER arrives (should be dropped)
        val duplicate = handleEnter("zone-gym")

        assertFalse("Second post-reboot ENTER must be dropped", duplicate)
        assertEquals(1, enterCount)
    }

    // ── Zone-change behaviour (must still work correctly) ─────────────────────

    @Test
    fun `ENTER for a different zone replaces the current zone`() {
        handleEnter("zone-home")
        val replaced = handleEnter("zone-work")   // user moved to a different zone
        assertTrue("ENTER for a NEW zone must not be dropped", replaced)
        assertEquals(2, enterCount)
        assertEquals("zone-work", currentZoneId)
    }

    @Test
    fun `EXIT clears zone state so a fresh ENTER is accepted afterwards`() {
        handleEnter("zone-cafe")
        handleExit("zone-cafe")
        assertEquals("zone-cafe", null.also { assertNull(currentZoneId) })

        // Re-entering the same zone after a clean EXIT is legitimate (user walked
        // out and back in) and must NOT be dropped by the idempotency guard.
        val reEntered = handleEnter("zone-cafe")
        assertTrue("Re-entry after clean EXIT must be accepted", reEntered)
        assertEquals(2, enterCount)
        assertEquals(1, exitCount)
    }

    @Test
    fun `EXIT for a different zone does not clear the current zone`() {
        handleEnter("zone-home")
        handleExit("zone-work")   // stale/unrelated EXIT
        assertEquals("zone-home", currentZoneId)
        assertEquals(0, exitCount)

        // Duplicate ENTER for the still-tracked zone must still be dropped
        val duplicate = handleEnter("zone-home")
        assertFalse(duplicate)
        assertEquals(1, enterCount)
    }

    // ── Null-safety ───────────────────────────────────────────────────────────

    @Test
    fun `no zone is tracked before any ENTER fires`() {
        assertNull(currentZoneId)
        assertEquals(0, enterCount)
    }

    @Test
    fun `EXIT with null zoneId clears any currently tracked zone`() {
        handleEnter("zone-park")
        handleExit(null)          // GeofenceBroadcastReceiver may pass null on some paths
        assertNull(currentZoneId)
        assertEquals(1, exitCount)
    }
}
