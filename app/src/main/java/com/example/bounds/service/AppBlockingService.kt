package com.example.bounds.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.bounds.R

/**
 * Foreground service that repeatedly kills a set of blocked apps until
 * [EXTRA_DURATION_MINUTES] elapses or the service is stopped externally.
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

    companion object {
        const val CHANNEL_ID              = "app_blocking_channel"
        const val NOTIFICATION_ID         = 1
        const val EXTRA_PACKAGE_NAME      = "package_name"       // single-app legacy
        const val EXTRA_BLOCKED_PACKAGES  = "blocked_packages"   // ArrayList<String>
        const val EXTRA_ZONE_NAME         = "zone_name"
        const val EXTRA_DURATION_MINUTES  = "duration_minutes"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        activityManager     = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

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

    private fun startBlockingLoop() {
        blockingRunnable = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() >= endTimeMillis) {
                    stopSelf()
                    return
                }
                killBlockedApps()
                val remaining = ((endTimeMillis - System.currentTimeMillis()) / 60_000L).toInt()
                updateNotification(remaining)
                handler.postDelayed(this, 500)
            }
        }
        blockingRunnable?.let { handler.post(it) }
    }

    private fun killBlockedApps() {
        try {
            val running = activityManager.runningAppProcesses ?: return
            for (proc in running) {
                if (blockedPackages.any { pkg -> proc.processName == pkg }) {
                    android.os.Process.killProcess(proc.pid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notificationTitle(): String {
        val prefix = if (zoneName.isNotBlank()) "🔒 $zoneName" else "🔒 Blocking active"
        return when (blockedPackages.size) {
            0    -> prefix
            1    -> "$prefix — ${appLabel(blockedPackages[0])}"
            else -> "$prefix — ${blockedPackages.size} apps"
        }
    }

    /** Returns a short human-readable label for a package name. */
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
