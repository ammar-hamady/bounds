package com.example.bounds.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.bounds.service.AppBlockingService

object AppBlockingManager {
    private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    private var isBlocking = false
    private var currentBlockedApp = ""

    fun startBlockingApp(
        context: Context,
        packageName: String = INSTAGRAM_PACKAGE,
        durationMinutes: Int
    ): Boolean {
        if (!isAppInstalled(context, packageName)) {
            return false
        }

        isBlocking = true
        currentBlockedApp = packageName

        val intent = Intent(context, AppBlockingService::class.java).apply {
            putExtra(AppBlockingService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AppBlockingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }

        return true
    }

    fun stopBlockingApp(context: Context) {
        val intent = Intent(context, AppBlockingService::class.java)
        context.stopService(intent)
        isBlocking = false
        currentBlockedApp = ""
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isCurrentlyBlocking(): Boolean = isBlocking

    fun getCurrentBlockedApp(): String = currentBlockedApp

    fun getInstagramPackageName(): String = INSTAGRAM_PACKAGE
}
