package com.example.bounds.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bounds.BoundsApplication
import com.example.bounds.util.BoundsGeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

/**
 * Re-registers all persisted geofences after a device reboot.
 *
 * Android clears every platform geofence on reboot. This receiver listens for
 * BOOT_COMPLETED (and LOCKED_BOOT_COMPLETED on Android 7+) and calls
 * [BoundsGeofenceManager.syncGeofences] with the zones read from DataStore so
 * enforcement resumes without requiring the user to open the app.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        Log.i(TAG, "Boot completed — re-registering geofences")

        val app = context.applicationContext as BoundsApplication

        // goAsync keeps the BroadcastReceiver alive while the coroutine runs.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val zones = app.zoneRepository.zonesFlow.first()
                // Keep the in-memory list in sync so GeofenceBroadcastReceiver
                // can resolve geofence IDs → zone data after the reboot.
                app.zones = zones
                BoundsGeofenceManager.syncGeofences(context, zones)
                Log.i(TAG, "Re-registered ${zones.count { it.isEnabled }} active geofence(s)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-register geofences after boot: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
