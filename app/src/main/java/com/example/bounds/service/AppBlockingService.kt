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

class AppBlockingService : Service() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var activityManager: ActivityManager
    private val handler = Handler(Looper.getMainLooper())
    private var blockingRunnable: Runnable? = null
    private var blockedApp: String = ""
    private var endTimeMillis: Long = 0

    companion object {
        const val CHANNEL_ID = "app_blocking_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_STICKY
            val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 30)

            blockedApp = packageName
            endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)

            startForeground(NOTIFICATION_ID, createNotification(durationMinutes))
            startBlockingApp()
        }
        return START_STICKY
    }

    private fun startBlockingApp() {
        blockingRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()

                if (currentTime >= endTimeMillis) {
                    stopSelf()
                    return
                }

                killBlockedApp()

                val remainingMinutes = (endTimeMillis - currentTime) / (60 * 1000)
                updateNotification(remainingMinutes.toInt())

                handler.postDelayed(this, 500)
            }
        }
        blockingRunnable?.let { handler.post(it) }
    }

    private fun killBlockedApp() {
        try {
            val runningApps = activityManager.runningAppProcesses ?: return
            for (appProcess in runningApps) {
                if (appProcess.processName == blockedApp) {
                    android.os.Process.killProcess(appProcess.pid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocking",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifications for blocked applications"
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(durationMinutes: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Instagram Blocked")
            .setContentText("Blocked for $durationMinutes minutes")
            .setSmallIcon(R.drawable.ic_favorite)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(remainingMinutes: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔒 Instagram Blocked")
            .setContentText("Remaining: $remainingMinutes minutes")
            .setSmallIcon(R.drawable.ic_favorite)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        blockingRunnable?.let { handler.removeCallbacks(it) }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
