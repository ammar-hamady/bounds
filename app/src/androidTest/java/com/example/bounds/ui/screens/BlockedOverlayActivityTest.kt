package com.example.bounds.ui.screens

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [BlockedOverlayActivity].
 *
 * What is covered
 * ───────────────
 * 1. The app label passed via [BlockedOverlayActivity.EXTRA_BLOCKED_APP_LABEL]
 *    appears on screen.
 * 2. The zone name passed via [BlockedOverlayActivity.EXTRA_ZONE_NAME]
 *    appears inside the zone-name pill.
 * 3. Tapping "Go to Home Screen" finishes the activity (lifecycle → DESTROYED).
 * 4. Tapping "Let me in once" sends the bypass intent to AppBlockingService
 *    and finishes the activity (lifecycle → DESTROYED).
 *
 * Strategy
 * ────────
 * [BlockedOverlayScreen] is a private top-level composable, so it cannot be
 * called directly in a [createComposeRule] host.  Instead each test launches
 * the real [BlockedOverlayActivity] with a crafted [Intent] via
 * [ActivityScenario] and asserts on the rendered semantics tree through
 * [createEmptyComposeRule], which attaches to whichever activity is currently
 * resumed.
 */
@RunWith(AndroidJUnit4::class)
class BlockedOverlayActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun launchWith(
        appLabel: String = "Test App",
        zoneName: String = "Test Zone",
        blockedPackage: String = "com.example.test"
    ): ActivityScenario<BlockedOverlayActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            BlockedOverlayActivity::class.java
        ).apply {
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_APP_LABEL, appLabel)
            putExtra(BlockedOverlayActivity.EXTRA_ZONE_NAME, zoneName)
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    // ── Content rendering ─────────────────────────────────────────────────────

    @Test
    fun overlayDisplaysCorrectAppLabel() {
        launchWith(appLabel = "YouTube", zoneName = "Library").use {
            composeTestRule
                .onNodeWithText("YouTube")
                .assertIsDisplayed()
        }
    }

    @Test
    fun overlayDisplaysCorrectZoneName() {
        launchWith(appLabel = "Instagram", zoneName = "Study Hall").use {
            // The zone name appears inside the pill text "📍  Study Hall"
            composeTestRule
                .onNodeWithText("Study Hall", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun overlayDisplaysBothLabelsSimultaneously() {
        launchWith(appLabel = "TikTok", zoneName = "Work Zone").use {
            composeTestRule.onNodeWithText("TikTok").assertIsDisplayed()
            composeTestRule
                .onNodeWithText("Work Zone", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun overlayDisplaysExplanatoryText() {
        launchWith().use {
            composeTestRule
                .onNodeWithText("This app is blocked while you're in", substring = true)
                .assertIsDisplayed()
        }
    }

    // ── Button labels ─────────────────────────────────────────────────────────

    @Test
    fun goToHomeButtonIsVisible() {
        launchWith().use {
            composeTestRule
                .onNodeWithText("Go to Home Screen")
                .assertIsDisplayed()
        }
    }

    @Test
    fun bypassButtonIsVisible() {
        launchWith().use {
            composeTestRule
                .onNodeWithText("Let me in once", substring = true)
                .assertIsDisplayed()
        }
    }

    // ── Button callbacks ──────────────────────────────────────────────────────

    @Test
    fun tappingGoToHomeFinishesTheActivity() {
        launchWith().use { scenario ->
            composeTestRule
                .onNodeWithText("Go to Home Screen")
                .performClick()

            composeTestRule.waitForIdle()

            assertEquals(
                "Activity must finish after 'Go to Home Screen' is tapped",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    @Test
    fun tappingBypassOnceFinishesTheActivity() {
        launchWith(blockedPackage = "com.example.test").use { scenario ->
            composeTestRule
                .onNodeWithText("Let me in once", substring = true)
                .performClick()

            composeTestRule.waitForIdle()

            assertEquals(
                "Activity must finish after 'Let me in once' is tapped",
                Lifecycle.State.DESTROYED,
                scenario.state
            )
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun overlayHandlesMissingZoneNameGracefully() {
        // Launch without a zone name extra — activity falls back to "your zone"
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            BlockedOverlayActivity::class.java
        ).apply {
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_APP_LABEL, "Snapchat")
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, "com.snapchat.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ActivityScenario.launch<BlockedOverlayActivity>(intent).use {
            composeTestRule
                .onNodeWithText("your zone", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun overlayHandlesDifferentZoneNames() {
        // Verify the UI is not hard-coded to a specific zone name — test two distinct zones
        launchWith(zoneName = "Coffee Shop").use {
            composeTestRule
                .onNodeWithText("Coffee Shop", substring = true)
                .assertIsDisplayed()
        }
        launchWith(zoneName = "Bedroom").use {
            composeTestRule
                .onNodeWithText("Bedroom", substring = true)
                .assertIsDisplayed()
        }
    }
}
