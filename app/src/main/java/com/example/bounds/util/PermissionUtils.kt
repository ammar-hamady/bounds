package com.example.bounds.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build

object PermissionUtils {

    /**
     * Returns true if the PACKAGE_USAGE_STATS special permission has been granted
     * (Settings > Apps > Special app access > Usage access).
     *
     * This is the same check used inside [AppBlockingService]; having it here lets
     * the UI layer query the state without depending on the service class.
     */
    fun hasUsageStatsPermission(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }
}
