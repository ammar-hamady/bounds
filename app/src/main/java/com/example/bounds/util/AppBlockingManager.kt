package com.example.bounds.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.bounds.service.AppBlockingService

/**
 * Convenience object for starting and stopping app-blocking sessions.
 *
 * Supports both:
 *  - Legacy single-package mode (used by CurrentScreen's manual lock button).
 *  - Zone-blocking mode (used by GeofenceEnforcementService) which maps
 *    friendly app names → package names and sends a list to AppBlockingService.
 */
object AppBlockingManager {

    // ── App name → package name mapping ──────────────────────────────────────
    private val NAME_TO_PACKAGE = mapOf(
        "Instagram" to "com.instagram.android",
        "TikTok"    to "com.zhiliaoapp.musically",
        "Twitter"   to "com.twitter.android",
        "Discord"   to "com.discord",
        "YouTube"   to "com.google.android.youtube",
        "Facebook"  to "com.facebook.katana",
        "Reddit"    to "com.reddit.frontpage",
        "Telegram"  to "org.telegram.messenger"
    )

    private const val INSTAGRAM_PACKAGE = "com.instagram.android"

    private var isBlocking = false
    private var currentBlockedPackages: List<String> = emptyList()

    // ── Single-app API (used by CurrentScreen) ────────────────────────────────

    /**
     * Starts blocking a single app by [packageName] for [durationMinutes].
     * Returns false if the app is not installed.
     */
    fun startBlockingApp(
        context: Context,
        packageName: String = INSTAGRAM_PACKAGE,
        durationMinutes: Int
    ): Boolean {
        if (!isAppInstalled(context, packageName)) return false
        isBlocking = true
        currentBlockedPackages = listOf(packageName)
        launchService(context, listOf(packageName), "", durationMinutes)
        return true
    }

    // ── Zone-blocking API (used by GeofenceEnforcementService) ────────────────

    /**
     * Starts blocking an app by its friendly [appName] (e.g. "Instagram").
     * If the package is not installed the call is silently skipped.
     */
    fun startBlockingByName(
        context: Context,
        appName: String,
        zoneName: String,
        durationMins: Int
    ) {
        val pkg = NAME_TO_PACKAGE[appName] ?: return
        if (!isAppInstalled(context, pkg)) return
        isBlocking = true
        currentBlockedPackages = currentBlockedPackages + pkg
        launchService(context, listOf(pkg), zoneName, durationMins)
    }

    /**
     * Stops ALL active blocking (stops AppBlockingService).
     */
    fun stopAllBlocking(context: Context) {
        context.stopService(Intent(context, AppBlockingService::class.java))
        isBlocking = false
        currentBlockedPackages = emptyList()
    }

    /** Legacy alias kept for CurrentScreen compatibility. */
    fun stopBlockingApp(context: Context) = stopAllBlocking(context)

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun isAppInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun isCurrentlyBlocking(): Boolean = isBlocking

    fun getCurrentBlockedApp(): String = currentBlockedPackages.firstOrNull() ?: ""

    fun getInstagramPackageName(): String = INSTAGRAM_PACKAGE

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun launchService(
        context: Context,
        packages: List<String>,
        zoneName: String,
        durationMinutes: Int
    ) {
        val intent = Intent(context, AppBlockingService::class.java).apply {
            putStringArrayListExtra(AppBlockingService.EXTRA_BLOCKED_PACKAGES, ArrayList(packages))
            putExtra(AppBlockingService.EXTRA_ZONE_NAME, zoneName)
            putExtra(AppBlockingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
