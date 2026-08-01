package com.example.bounds.service

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bounds.R
import com.example.bounds.ui.screens.BlockedOverlayActivity

private const val TAG = "AppBlockingService"

/**
 * Foreground service that enforces app blocking while inside a geofenced zone.
 *
 * Strategy:
 *  1. Every 500 ms it checks which app is currently in the foreground via
 *     [UsageStatsManager] (falls back to [ActivityManager.runningAppProcesses]).
 *  2. If a blocked app is detected in the foreground AND no overlay is already
 *     showing for it, it launches [BlockedOverlayActivity] so the user sees a
 *     clear explanation instead of a silent crash.
 *  3. A "bypass once" window (default 5 min) can be granted per-package via
 *     [ACTION_BYPASS_ONCE]; during that window the package is not interrupted.
 *
 * Started by [AppBlockingManager]; stopped via [AppBlockingManager.stopAllBlocking].
 */
class AppBlockingService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var activityManager: ActivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var blockingRunnable: Runnable? = null

    private var blockedPackages: List<String> = emptyList()
    private var zoneName: String = ""
    private var endTimeMillis: Long = 0L

    /** Package currently shown in the overlay so we don't re-launch it each tick. */
    private var overlayShownForPackage: String? = null

    /** Per-package bypass expiry timestamps (millis). */
    private val bypassExpiry: MutableMap<String, Long> = mutableMapOf()

    companion object {
        const val CHANNEL_ID              = "app_blocking_channel"
        const val NOTIFICATION_ID         = 1
        const val EXTRA_PACKAGE_NAME      = "package_name"       // single-app legacy
        const val EXTRA_BLOCKED_PACKAGES  = "blocked_packages"   // ArrayList<String>
        const val EXTRA_ZONE_NAME         = "zone_name"
        const val EXTRA_DURATION_MINUTES  = "duration_minutes"

        /** Sent by [BlockedOverlayActivity] to grant a one-time 5-minute bypass. */
        const val ACTION_BYPASS_ONCE    = "com.example.bounds.ACTION_BYPASS_ONCE"
        const val EXTRA_BYPASS_PACKAGE  = "bypass_package"

        private const val BYPASS_DURATION_MS = 5 * 60_000L   // 5 minutes
        private const val POLL_INTERVAL_MS   = 500L
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        activityManager     = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        // Handle bypass-once request from BlockedOverlayActivity
        if (intent.action == ACTION_BYPASS_ONCE) {
            val pkg = intent.getStringExtra(EXTRA_BYPASS_PACKAGE)
            if (pkg != null) {
                bypassExpiry[pkg] = System.currentTimeMillis() + BYPASS_DURATION_MS
                Log.i(TAG, "Bypass granted for $pkg (5 min)")
                // Clear overlay tracking so it can re-show after the bypass window
                if (overlayShownForPackage == pkg) overlayShownForPackage = null
            }
            return START_STICKY
        }

        val duration = intent.getIntExtra(EXTRA_DURATION_MINUTES, 30)
        zoneName = intent.getStringExtra(EXTRA_ZONE_NAME) ?: ""

        // Support both single-package (legacy) and multi-package modes
        val pkgList = intent.getStringArrayListExtra(EXTRA_BLOCKED_PACKAGES)
        blockedPackages = when {
            pkgList != null && pkgList.isNotEmpty() -> pkgList
            else -> {
                val single = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (single != null) listOf(single) else return START_STICKY
            }
        }

        endTimeMillis = System.currentTimeMillis() + duration * 60_000L

        startForeground(NOTIFICATION_ID, buildNotification(duration))
        startBlockingLoop()
        return START_STICKY
    }

    // ── Blocking loop ─────────────────────────────────────────────────────────

    private fun startBlockingLoop() {
        blockingRunnable = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() >= endTimeMillis) {
                    stopSelf()
                    return
                }
                enforceBlocking()
                val remaining = ((endTimeMillis - System.currentTimeMillis()) / 60_000L).toInt()
                updateNotification(remaining)
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        blockingRunnable?.let { handler.post(it) }
    }

    /**
     * Main enforcement tick: if a blocked (non-bypassed) app is in the foreground,
     * show the overlay explanation. Otherwise clear the overlay-shown tracker so it
     * can re-fire if the same app is opened again later.
     */
    private fun enforceBlocking() {
        val foreground = getForegroundPackage()

        val blockedForeground = foreground?.let { fg ->
            blockedPackages.firstOrNull { pkg -> pkg == fg }
        }

        if (blockedForeground != null) {
            val now = System.currentTimeMillis()
            val bypassUntil = bypassExpiry[blockedForeground] ?: 0L
            if (now < bypassUntil) {
                // Within bypass window — do not interrupt
                if (overlayShownForPackage == blockedForeground) overlayShownForPackage = null
                return
            }
            // Show overlay only once per foreground session for this package
            if (overlayShownForPackage != blockedForeground) {
                overlayShownForPackage = blockedForeground
                showBlockedOverlay(blockedForeground)
            }
        } else {
            // Blocked app is no longer in foreground — reset so overlay fires again next time
            if (overlayShownForPackage != null && overlayShownForPackage !in (blockedPackages)) {
                overlayShownForPackage = null
            } else if (foreground != null && overlayShownForPackage != null &&
                foreground != overlayShownForPackage) {
                overlayShownForPackage = null
            }
        }
    }

    /**
     * Returns the package name of the app currently in the foreground, or null.
     *
     * Prefers [UsageStatsManager] (accurate on API 21+, requires PACKAGE_USAGE_STATS).
     * Falls back to [ActivityManager.runningAppProcesses] on older/restricted devices.
     */
    private fun getForegroundPackage(): String? {
        if (hasUsageStatsPermission()) {
            try {
                val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 10_000L,
                    now
                )
                val topStat = stats?.filter { it.lastTimeUsed > 0 }
                    ?.maxByOrNull { it.lastTimeUsed }
                if (topStat != null) return topStat.packageName
            } catch (e: Exception) {
                Log.w(TAG, "UsageStatsManager failed: ${e.message}")
            }
        }

        // Fallback: scan running processes (less reliable for foreground detection)
        return try {
            activityManager.runningAppProcesses
                ?.firstOrNull { it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
                ?.processName
        } catch (e: Exception) {
            null
        }
    }

    /** Checks whether the app has been granted the PACKAGE_USAGE_STATS special permission. */
    private fun hasUsageStatsPermission(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            val ops = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            val mode = ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        }
    } catch (e: Exception) {
        false
    }

    /** Launches [BlockedOverlayActivity] on top of the blocked app. */
    private fun showBlockedOverlay(blockedPackage: String) {
        val appLabel = try {
            val info = packageManager.getApplicationInfo(blockedPackage, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            blockedPackage.substringAfterLast('.')
        }

        Log.i(TAG, "Showing blocked overlay for $blockedPackage in zone '$zoneName'")

        val overlayIntent = Intent(this, BlockedOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_APP_LABEL, appLabel)
            putExtra(BlockedOverlayActivity.EXTRA_ZONE_NAME, zoneName)
        }
        startActivity(overlayIntent)
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun notificationTitle(): String {
        val prefix = if (zoneName.isNotBlank()) "🔒 $zoneName" else "🔒 Blocking active"
        return when (blockedPackages.size) {
            0    -> prefix
            1    -> "$prefix — ${appLabel(blockedPackages[0])}"
            else -> "$prefix — ${blockedPackages.size} apps"
        }
    }

    private fun appLabel(pkg: String): String {
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg.substringAfterLast('.')
        }
    }

    private fun buildNotification(durationMinutes: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle())
            .setContentText("Blocked for $durationMinutes minutes")
            .setSmallIcon(R.drawable.ic_favorite)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun updateNotification(remainingMinutes: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle())
            .setContentText("Remaining: $remainingMinutes min")
            .setSmallIcon(R.drawable.ic_favorite)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "App Blocking", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Notifications for blocked applications" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blockingRunnable?.let { handler.removeCallbacks(it) }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
