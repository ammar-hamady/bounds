package com.example.bounds.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bounds.BoundsApplication
import com.example.bounds.R
import com.example.bounds.model.ActiveEnforcementInfo
import com.example.bounds.model.AnalyticsEvent
import com.example.bounds.util.AppBlockingManager
import java.util.UUID

private const val TAG = "GeofenceEnfService"

/**
 * Foreground service that orchestrates zone enforcement:
 *  1. On ENTER — shows a notification, starts the grace-period countdown.
 *  2. After grace expires — starts AppBlockingService for each blocked app.
 *  3. On EXIT — cancels any pending grace timer, stops blocking, records analytics.
 *
 * Multiple zones are handled by receiving additional ENTER commands; the latest
 * zone always wins (the app keeps state for one enforced zone at a time).
 */
class GeofenceEnforcementService : Service() {

    companion object {
        const val ACTION_ZONE_ENTER = "com.example.bounds.ACTION_ZONE_ENTER"
        const val ACTION_ZONE_EXIT  = "com.example.bounds.ACTION_ZONE_EXIT"

        const val EXTRA_ZONE_ID          = "zone_id"
        const val EXTRA_ZONE_NAME        = "zone_name"
        const val EXTRA_BLOCKED_APPS     = "blocked_apps"
        const val EXTRA_IS_TIME_SENSITIVE = "is_time_sensitive"
        const val EXTRA_START_TIME       = "start_time"
        const val EXTRA_END_TIME         = "end_time"

        private const val CHANNEL_ID        = "bounds_enforcement_channel"
        private const val NOTIFICATION_ID   = 3
    }

    private val handler = Handler(Looper.getMainLooper())
    private var graceRunnable: Runnable? = null

    private var currentZoneId: String?     = null
    private var currentZoneName: String?   = null
    private var blockedApps: List<String>  = emptyList()
    private var isActivelyBlocking         = false
    private var blockingStartedAtMs: Long  = 0L

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ZONE_ENTER -> handleEnter(intent)
            ACTION_ZONE_EXIT  -> handleExit(intent.getStringExtra(EXTRA_ZONE_ID))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        graceRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Enter ─────────────────────────────────────────────────────────────────

    private fun handleEnter(intent: Intent) {
        val zoneId   = intent.getStringExtra(EXTRA_ZONE_ID)   ?: return
        val zoneName = intent.getStringExtra(EXTRA_ZONE_NAME) ?: "Zone"
        val apps     = intent.getStringArrayListExtra(EXTRA_BLOCKED_APPS) ?: ArrayList()
        val timeSensitive = intent.getBooleanExtra(EXTRA_IS_TIME_SENSITIVE, false)
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: "00:00"
        val endTime   = intent.getStringExtra(EXTRA_END_TIME)   ?: "23:59"

        // Respect time-sensitive window
        if (timeSensitive && !isWithinTimeWindow(startTime, endTime)) {
            Log.d(TAG, "Zone '$zoneName' is time-sensitive but outside window — skipping")
            return
        }

        // Cancel any previous grace countdown
        graceRunnable?.let { handler.removeCallbacks(it) }

        currentZoneId   = zoneId
        currentZoneName = zoneName
        blockedApps     = apps

        val app = applicationContext as BoundsApplication
        val graceMs = app.graceTimerSeconds * 1000L

        // Start foreground immediately (required on API 26+)
        val graceLabel = if (graceMs > 0) "Grace period: ${app.graceTimerSeconds}s…" else "Blocking active"
        startForeground(NOTIFICATION_ID, buildNotification("Entering $zoneName", graceLabel))

        app.setEnforcement(
            ActiveEnforcementInfo(
                zoneId      = zoneId,
                zoneName    = zoneName,
                blockedApps = apps,
                isGracePeriod = graceMs > 0
            )
        )

        Log.i(TAG, "Zone ENTER: '$zoneName', grace=${app.graceTimerSeconds}s, apps=$apps")

        graceRunnable = Runnable { activateBlocking(zoneId, zoneName, apps) }
        if (graceMs > 0) {
            handler.postDelayed(graceRunnable!!, graceMs)
        } else {
            graceRunnable!!.run()
        }
    }

    private fun activateBlocking(zoneId: String, zoneName: String, apps: List<String>) {
        if (currentZoneId != zoneId) return   // zone changed during grace period

        Log.i(TAG, "Activating blocking for '$zoneName': $apps")
        apps.forEach { appName ->
            AppBlockingManager.startBlockingByName(
                context      = applicationContext,
                appName      = appName,
                zoneName     = zoneName,
                durationMins = 60   // long sentinel; stopped explicitly on EXIT
            )
        }

        // Haptic: double pulse to signal enforcement start
        if ((applicationContext as BoundsApplication).hapticFeedbackEnabled) {
            vibrate(longArrayOf(0, 100, 50, 100))
        }

        blockingStartedAtMs = System.currentTimeMillis()
        isActivelyBlocking  = true

        (applicationContext as BoundsApplication).setEnforcement(
            ActiveEnforcementInfo(
                zoneId      = zoneId,
                zoneName    = zoneName,
                blockedApps = apps,
                isGracePeriod = false
            )
        )

        val appsLabel = when {
            apps.isEmpty() -> "No apps configured"
            apps.size == 1 -> "${apps[0]} blocked"
            else           -> "${apps[0]} + ${apps.size - 1} more blocked"
        }
        updateNotification("🔒 $zoneName active", appsLabel)
    }

    // ── Exit ──────────────────────────────────────────────────────────────────

    private fun handleExit(zoneId: String?) {
        if (zoneId != null && zoneId != currentZoneId) return   // exit for a different zone

        Log.i(TAG, "Zone EXIT: '$currentZoneName'")

        graceRunnable?.let { handler.removeCallbacks(it) }
        graceRunnable = null

        AppBlockingManager.stopAllBlocking(applicationContext)

        val app = applicationContext as BoundsApplication

        // Record analytics for each app that was actually blocked
        if (isActivelyBlocking && blockingStartedAtMs > 0) {
            val durationMins = ((System.currentTimeMillis() - blockingStartedAtMs) / 60_000L)
                .toInt().coerceAtLeast(1)
            blockedApps.forEach { appName ->
                app.postAnalyticsEvent(
                    AnalyticsEvent(
                        id              = UUID.randomUUID().toString(),
                        appName         = appName,
                        zoneName        = currentZoneName ?: "Unknown",
                        durationMinutes = durationMins,
                        timestampMs     = System.currentTimeMillis()
                    )
                )
            }
        }

        // Haptic: single short pulse to signal zone clear
        if (app.hapticFeedbackEnabled) {
            vibrate(longArrayOf(0, 80))
        }

        app.setEnforcement(null)
        isActivelyBlocking   = false
        currentZoneId        = null
        currentZoneName      = null
        blockedApps          = emptyList()
        blockingStartedAtMs  = 0L

        @Suppress("DEPRECATION")
        stopForeground(true)
        stopSelf()
    }

    // ── Vibration helper ──────────────────────────────────────────────────────

    /**
     * Vibrates with the given pattern (format: [delay, on, off, on, …]).
     * Uses VibrationEffect on API 26+ and falls back to the deprecated API below.
     */
    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = manager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    // ── Time-window helper ────────────────────────────────────────────────────

    private fun isWithinTimeWindow(startTime: String, endTime: String): Boolean {
        return try {
            val now       = java.util.Calendar.getInstance()
            val nowMins   = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            val startMins = parseTimeMins(startTime)
            val endMins   = parseTimeMins(endTime)
            if (startMins <= endMins) nowMins in startMins..endMins
            else                      nowMins >= startMins || nowMins <= endMins  // wraps midnight
        } catch (e: Exception) {
            true  // fail open
        }
    }

    private fun parseTimeMins(time: String): Int {
        val parts = time.split(":")
        return parts[0].trim().toInt() * 60 + parts.getOrElse(1) { "0" }.trim().toInt()
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zone Enforcement",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Active geofence enforcement status" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_favorite)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun updateNotification(title: String, text: String) {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIFICATION_ID, buildNotification(title, text))
    }
}
