package com.example.bounds.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppBlockingService's overlay-dispatch and bypass-window logic.
 *
 * Context / motivation
 * ─────────────────────
 * AppBlockingService polls the foreground app every 500 ms and shows
 * BlockedOverlayActivity when a blocked package is detected.  Two invariants
 * must hold for a good user experience:
 *
 *  1. **Single-show per session** — the overlay is launched at most once per
 *     continuous foreground session for a given package (tracked by
 *     overlayShownForPackage).  Without this guard the overlay would reappear
 *     every 500 ms while the user is looking at it.
 *
 *  2. **Bypass window** — after the user taps "Let me in once", a 5-minute
 *     window is recorded in bypassExpiry.  During that window the package must
 *     not be interrupted even if it remains in the foreground.  After the
 *     window expires the overlay must re-appear if the package is still (or
 *     again) in the foreground.
 *
 * These tests model the state machine directly, mirroring the production
 * fields and the guard logic inside enforceBlocking(), without pulling in the
 * Android Service framework.  This follows the same strategy used in
 * GeofenceEnforcementIdempotencyTest.
 */
class AppBlockingEnforcementTest {

    // ── Minimal state-machine model ───────────────────────────────────────────
    //
    // Mirrors the fields in AppBlockingService that govern overlay visibility
    // and bypass tracking.

    /** Package currently shown in the overlay (null = no overlay showing). */
    private var overlayShownForPackage: String? = null

    /** Per-package bypass expiry timestamps (millis). */
    private val bypassExpiry: MutableMap<String, Long> = mutableMapOf()

    /** Count of times showBlockedOverlay() would have been called. */
    private var overlayLaunchCount: Int = 0

    // ── Injected clock ────────────────────────────────────────────────────────

    /** Mutable "current time" so tests can fast-forward without Thread.sleep. */
    private var nowMillis: Long = 1_000_000L

    private fun now() = nowMillis

    // ── State-machine helpers ─────────────────────────────────────────────────

    /**
     * Simulates AppBlockingService.enforceBlocking() for a single tick.
     *
     * [foreground] is the package currently in the foreground (null = home/unknown).
     * [blockedPackages] is the current list of packages the zone restricts.
     */
    private fun tick(foreground: String?, blockedPackages: List<String>) {
        val blockedForeground = foreground?.let { fg ->
            blockedPackages.firstOrNull { it == fg }
        }

        if (blockedForeground != null) {
            val bypassUntil = bypassExpiry[blockedForeground] ?: 0L
            if (now() < bypassUntil) {
                // Within bypass window — do not interrupt
                if (overlayShownForPackage == blockedForeground) overlayShownForPackage = null
                return
            }
            // Show overlay only once per foreground session for this package
            if (overlayShownForPackage != blockedForeground) {
                overlayShownForPackage = blockedForeground
                overlayLaunchCount++
            }
        } else {
            // Blocked app left foreground — reset tracker so overlay fires again next time
            if (overlayShownForPackage != null && overlayShownForPackage !in blockedPackages) {
                overlayShownForPackage = null
            } else if (foreground != null && overlayShownForPackage != null &&
                foreground != overlayShownForPackage) {
                overlayShownForPackage = null
            }
        }
    }

    /**
     * Simulates the bypass intent arriving from BlockedOverlayActivity
     * (mirrors onStartCommand ACTION_BYPASS_ONCE handling).
     */
    private fun grantBypass(pkg: String, durationMs: Long = BYPASS_DURATION_MS) {
        bypassExpiry[pkg] = now() + durationMs
        if (overlayShownForPackage == pkg) overlayShownForPackage = null
    }

    @Before
    fun setUp() {
        overlayShownForPackage = null
        bypassExpiry.clear()
        overlayLaunchCount = 0
        nowMillis = 1_000_000L
    }

    // ── Single-show per foreground session ────────────────────────────────────

    @Test
    fun `overlay is launched on the first tick a blocked app is in the foreground`() {
        val blocked = listOf("com.example.blocked")
        tick(foreground = "com.example.blocked", blockedPackages = blocked)
        assertEquals("Overlay must launch once on first detection", 1, overlayLaunchCount)
        assertEquals("com.example.blocked", overlayShownForPackage)
    }

    @Test
    fun `overlay is not re-launched while the same app stays in the foreground`() {
        val blocked = listOf("com.example.blocked")
        repeat(10) {
            tick(foreground = "com.example.blocked", blockedPackages = blocked)
        }
        assertEquals(
            "Overlay must not re-launch every poll tick for the same foreground session",
            1,
            overlayLaunchCount
        )
    }

    @Test
    fun `overlay fires again after blocked app briefly leaves the foreground`() {
        val pkg = "com.example.blocked"
        val blocked = listOf(pkg)

        // First foreground session
        tick(foreground = pkg, blockedPackages = blocked)
        assertEquals(1, overlayLaunchCount)

        // App moves to background (home screen)
        tick(foreground = "com.android.launcher3", blockedPackages = blocked)
        assertNull("Tracker must clear when app leaves foreground", overlayShownForPackage)

        // App returns to foreground → overlay must re-appear
        tick(foreground = pkg, blockedPackages = blocked)
        assertEquals(
            "Overlay must re-launch when blocked app returns to foreground",
            2,
            overlayLaunchCount
        )
    }

    // ── Bypass window ─────────────────────────────────────────────────────────

    @Test
    fun `overlay is not shown during the bypass window`() {
        val pkg = "com.example.blocked"
        val blocked = listOf(pkg)

        grantBypass(pkg)

        // Multiple ticks while bypass is active — overlay must stay silent
        repeat(5) {
            nowMillis += 10_000L  // advance 10 s per tick (still within 5 min bypass)
            tick(foreground = pkg, blockedPackages = blocked)
        }

        assertEquals(
            "No overlay launch expected during bypass window",
            0,
            overlayLaunchCount
        )
    }

    @Test
    fun `bypass window clears overlayShownForPackage so it can re-fire later`() {
        val pkg = "com.example.blocked"
        val blocked = listOf(pkg)

        // Overlay was showing when bypass was granted
        overlayShownForPackage = pkg
        overlayLaunchCount = 1

        grantBypass(pkg)

        assertNull(
            "Granting bypass must clear overlayShownForPackage so re-show is possible after expiry",
            overlayShownForPackage
        )
    }

    @Test
    fun `overlay re-appears after the bypass window expires`() {
        val pkg = "com.example.blocked"
        val blocked = listOf(pkg)

        grantBypass(pkg, durationMs = BYPASS_DURATION_MS)

        // Tick near end of window — still silent
        nowMillis += BYPASS_DURATION_MS - 1_000L
        tick(foreground = pkg, blockedPackages = blocked)
        assertEquals("Overlay must not launch while bypass is still active", 0, overlayLaunchCount)

        // Advance past the bypass window
        nowMillis += 2_000L
        tick(foreground = pkg, blockedPackages = blocked)
        assertEquals(
            "Overlay must re-launch once the bypass window has expired",
            1,
            overlayLaunchCount
        )
    }

    @Test
    fun `bypass for one package does not suppress the overlay for a different blocked package`() {
        val bypassedPkg = "com.example.bypassed"
        val otherPkg    = "com.example.other"
        val blocked = listOf(bypassedPkg, otherPkg)

        grantBypass(bypassedPkg)

        // The non-bypassed package is in the foreground
        tick(foreground = otherPkg, blockedPackages = blocked)
        assertEquals(
            "Overlay must launch for a package that has no active bypass",
            1,
            overlayLaunchCount
        )
        assertEquals(otherPkg, overlayShownForPackage)
    }

    // ── Multiple blocked packages ─────────────────────────────────────────────

    @Test
    fun `overlay fires for each distinct blocked package when brought to foreground`() {
        val pkgA  = "com.example.pkgA"
        val pkgB  = "com.example.pkgB"
        val blocked = listOf(pkgA, pkgB)

        // pkgA comes to foreground
        tick(foreground = pkgA, blockedPackages = blocked)
        assertEquals(1, overlayLaunchCount)

        // User switches to pkgB
        tick(foreground = pkgB, blockedPackages = blocked)
        assertEquals(
            "Overlay must fire again when a different blocked package enters the foreground",
            2,
            overlayLaunchCount
        )
    }

    @Test
    fun `no overlay when foreground app is not in the blocked list`() {
        val blocked = listOf("com.example.blocked")
        tick(foreground = "com.example.allowed", blockedPackages = blocked)
        assertEquals(0, overlayLaunchCount)
        assertNull(overlayShownForPackage)
    }

    @Test
    fun `no overlay when foreground is null`() {
        val blocked = listOf("com.example.blocked")
        tick(foreground = null, blockedPackages = blocked)
        assertEquals(0, overlayLaunchCount)
        assertNull(overlayShownForPackage)
    }

    companion object {
        private const val BYPASS_DURATION_MS = 5 * 60_000L
    }
}
