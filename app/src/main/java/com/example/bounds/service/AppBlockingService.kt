package com.example.bounds.service

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.bounds.R

private const val TAG = "AppBlockingService"
private val AMBER = Color.rgb(255, 193, 7)

/**
 * Foreground service that enforces app blocking while inside a geofenced zone.
 *
 * Strategy:
 *  1. Every 500 ms it checks which app is currently in the foreground via
 *     recent [UsageEvents] from [UsageStatsManager].
 *  2. If a blocked app is detected in the foreground, it presents a real
 *     TYPE_APPLICATION_OVERLAY. A normal Activity launch from a background
 *     service is restricted by modern Android and can be silently ignored.
 *  3. A "bypass once" window (default 5 min) can be granted per-package via
 *     [ACTION_BYPASS_ONCE]; during that window the package is not interrupted.
 *
 * Started by [AppBlockingManager]; stopped via [AppBlockingManager.stopAllBlocking].
 */
class AppBlockingService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var blockingRunnable: Runnable? = null
    private var blockingOverlayView: View? = null
    private var blockingOverlayPackage: String? = null

    private var blockedPackages: List<String> = emptyList()
    private var zoneName: String = ""
    private var endTimeMillis: Long = 0L

    /**
     * Set of packages whose overlay is currently active on screen.
     * Using a Set lets us track multiple blocked apps independently so
     * switching between two blocked apps doesn't cause stacking or missed
     * overlay launches.
     */
    private val overlayActiveFor: MutableSet<String> = mutableSetOf()

    /**
     * Per-package timestamp (millis) of the last time we launched the overlay.
     * A new launch is suppressed for [OVERLAY_COOLDOWN_MS] after the previous one
     * to prevent rapid re-firing when the polling loop ticks faster than the
     * Activity transition completes.
     */
    private val overlayLastShown: MutableMap<String, Long> = mutableMapOf()

    /** Per-package bypass expiry timestamps (millis). */
    private val bypassExpiry: MutableMap<String, Long> = mutableMapOf()

    companion object {
        const val CHANNEL_ID              = "app_blocking_channel"
        const val NOTIFICATION_ID         = 1
        const val EXTRA_PACKAGE_NAME      = "package_name"       // single-app legacy
        const val EXTRA_BLOCKED_PACKAGES  = "blocked_packages"   // ArrayList<String>
        const val EXTRA_ZONE_NAME         = "zone_name"
        const val EXTRA_DURATION_MINUTES  = "duration_minutes"

        /** Grants a one-time five-minute bypass for one package. */
        const val ACTION_BYPASS_ONCE    = "com.example.bounds.ACTION_BYPASS_ONCE"
        const val EXTRA_BYPASS_PACKAGE  = "bypass_package"

        private const val BYPASS_DURATION_MS  = 5 * 60_000L   // 5 minutes
        private const val POLL_INTERVAL_MS    = 500L
        /** Minimum gap between two overlay launches for the same package. */
        private const val OVERLAY_COOLDOWN_MS = 3_000L        // 3 seconds
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        // Handle bypass-once requests from either overlay implementation.
        if (intent.action == ACTION_BYPASS_ONCE) {
            val pkg = intent.getStringExtra(EXTRA_BYPASS_PACKAGE)
            if (pkg != null) {
                grantBypass(pkg)
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
        blockingRunnable?.let { handler.removeCallbacks(it) }
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
     * show the overlay explanation. Each package is tracked independently in
     * [overlayActiveFor] so switching between two blocked apps never causes the
     * overlay to stack or be skipped.
     *
     * A short [OVERLAY_COOLDOWN_MS] guard per package prevents rapid re-launches
     * while the Activity transition is still completing.
     *
     * Packages that are no longer in the foreground are removed from [overlayActiveFor]
     * so the overlay fires again the next time the user opens them.
     */
    private fun enforceBlocking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Android removes application-overlay windows when this permission
            // is revoked. Clear our bookkeeping too so enforcement can recover
            // after the user grants it again.
            overlayActiveFor.clear()
            hideBlockingOverlay()
            return
        }

        val foreground = getForegroundPackage()
        val now = System.currentTimeMillis()

        // Bounds must never block or overlay itself. This also protects against
        // stale usage data briefly reporting a previously opened app while the
        // Bounds screen is still in the foreground.
        if (foreground == packageName) {
            val departed = overlayActiveFor.toSet()
            overlayActiveFor.clear()
            hideBlockingOverlay()
            if (departed.isNotEmpty()) {
                Log.d(TAG, "Cleared overlay tracking while Bounds is foreground: $departed")
            }
            return
        }

        val blockedForeground = foreground?.let { fg ->
            blockedPackages.firstOrNull { pkg -> pkg == fg }
        }

        if (blockedForeground != null) {
            val bypassUntil = bypassExpiry[blockedForeground] ?: 0L
            if (now < bypassUntil) {
                // Within bypass window — treat as if not blocked; clear active flag
                overlayActiveFor.remove(blockedForeground)
                if (blockingOverlayPackage == blockedForeground) hideBlockingOverlay()
                return
            }

            // Only launch overlay if:
            //  (a) we haven't already shown it for this package (it's still on screen), AND
            //  (b) the per-package cooldown has elapsed (guards against transition flicker)
            val lastShown = overlayLastShown[blockedForeground] ?: 0L
            if (blockedForeground !in overlayActiveFor &&
                (now - lastShown) >= OVERLAY_COOLDOWN_MS
            ) {
                if (showBlockedOverlay(blockedForeground)) {
                    overlayActiveFor.add(blockedForeground)
                    overlayLastShown[blockedForeground] = now
                    Log.d(TAG, "overlayActiveFor=$overlayActiveFor")
                }
            }
        } else {
            // The currently-foreground app is not blocked (or unknown).
            // Remove any package from the active set that is no longer in the foreground
            // so the overlay will fire again the next time the user opens it.
            if (foreground != null) {
                // Only clear the package that just left the foreground, not everything.
                // We identify it as whichever active packages are NOT the current foreground.
                val departed = overlayActiveFor.filter { it != foreground }
                if (departed.isNotEmpty()) {
                    overlayActiveFor.removeAll(departed.toSet())
                    hideBlockingOverlay()
                    Log.d(TAG, "Cleared overlay tracking for departed: $departed")
                }
            }
        }
    }

    /**
     * Returns the package name of the app currently in the foreground, or null.
     *
     * Uses recent [UsageEvents] transitions (requires PACKAGE_USAGE_STATS).
     * There is intentionally no process-list fallback: modern Android does not
     * guarantee that running-process importance identifies the visible app.
     */
    private fun getForegroundPackage(): String? {
        if (!hasUsageStatsPermission()) {
            // The running-process fallback cannot reliably identify the visible
            // app on modern Android and can silently make blocking ineffective.
            return null
        }

        try {
            val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 5_000L, now)
            val event = UsageEvents.Event()
            var latestPackage: String? = null
            var latestTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                } else {
                    @Suppress("DEPRECATION")
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }

                if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                    latestTimestamp = event.timeStamp
                    latestPackage = event.packageName
                }
            }

            if (latestPackage != null) return latestPackage
        } catch (e: Exception) {
            Log.w(TAG, "UsageStatsManager failed: ${e.message}")
        }

        return null
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

    /**
     * Adds an interactive system overlay above the blocked app. Unlike starting
     * an Activity from this background service, this remains reliable under
     * Android's background-activity-launch restrictions.
     */
    private fun showBlockedOverlay(blockedPackage: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Cannot show block screen: overlay permission is not granted")
            return false
        }
        if (blockingOverlayView != null && blockingOverlayPackage == blockedPackage) return true

        hideBlockingOverlay()
        val view = createBlockingOverlay(blockedPackage)
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        return try {
            windowManager.addView(view, params)
            blockingOverlayView = view
            blockingOverlayPackage = blockedPackage
            Log.i(TAG, "Showing system overlay for $blockedPackage in zone '$zoneName'")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show blocking overlay", e)
            false
        }
    }

    private fun createBlockingOverlay(blockedPackage: String): View {
        val appName = appLabel(blockedPackage)
        val contextLabel = zoneName.takeIf { it.isNotBlank() } ?: "Manual Block"

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(248, 16, 16, 16))
            isClickable = true
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(28), dp(28), dp(28))
            background = roundedBackground(Color.rgb(26, 26, 26), 20f)
        }
        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            leftMargin = dp(28)
            rightMargin = dp(28)
        }

        card.addView(overlayText("🔒", 48f, Color.WHITE, 0))
        card.addView(overlayText(appName, 22f, AMBER, dp(12)))
        card.addView(
            overlayText(
                "This app is blocked while Bounds is active",
                14f,
                Color.rgb(187, 187, 187),
                dp(14)
            )
        )
        card.addView(overlayText("📍  $contextLabel", 14f, AMBER, dp(12)))

        val homeButton = Button(this).apply {
            text = "Go to Home Screen"
            isAllCaps = false
            setTextColor(Color.rgb(16, 16, 16))
            textSize = 15f
            background = roundedBackground(AMBER, 12f)
            setOnClickListener {
                try {
                    // Keep the overlay attached while starting Home. Android 15
                    // requires a visible overlay for this background launch.
                    // The next usage-event tick removes it after Home resumes.
                    startActivity(Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open the Home screen", e)
                }
            }
        }
        card.addView(homeButton, buttonLayoutParams(dp(20)))

        val bypassButton = Button(this).apply {
            text = "Let me in once (5 min)"
            isAllCaps = false
            setTextColor(Color.LTGRAY)
            textSize = 14f
            background = roundedBackground(Color.rgb(45, 45, 45), 12f)
            setOnClickListener { grantBypass(blockedPackage) }
        }
        card.addView(bypassButton, buttonLayoutParams(dp(10)))
        root.addView(card, cardParams)
        return root
    }

    private fun overlayText(textValue: String, sizeSp: Float, color: Int, topMargin: Int): TextView =
        TextView(this).apply {
            text = textValue
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = topMargin
            layoutParams = params
        }

    private fun buttonLayoutParams(topMargin: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(52)
        ).apply { this.topMargin = topMargin }

    private fun roundedBackground(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp.toInt()).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun grantBypass(blockedPackage: String) {
        bypassExpiry[blockedPackage] = System.currentTimeMillis() + BYPASS_DURATION_MS
        overlayActiveFor.remove(blockedPackage)
        overlayLastShown.remove(blockedPackage)
        hideBlockingOverlay()
        Log.i(TAG, "Bypass granted for $blockedPackage (5 min)")
    }

    private fun hideBlockingOverlay() {
        blockingOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Blocking overlay was already removed")
            }
        }
        blockingOverlayView = null
        blockingOverlayPackage = null
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
        hideBlockingOverlay()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
