package com.example.bounds.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.bounds.BoundsApplication
import com.example.bounds.service.GeofenceEnforcementService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

private const val TAG = "GeofenceReceiver"

/**
 * Receives geofence enter/exit transitions from the Play Services runtime.
 * Translates them into start/stop commands for [GeofenceEnforcementService].
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "GeofencingEvent error code: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggered = event.triggeringGeofences ?: return
        val app = context.applicationContext as BoundsApplication

        Log.d(TAG, "Transition=$transition, zones=${triggered.map { it.requestId }}")

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                for (geofence in triggered) {
                    val zone = app.zones.find { it.id == geofence.requestId }
                    if (zone == null || !zone.isEnabled) continue

                    val serviceIntent = Intent(context, GeofenceEnforcementService::class.java).apply {
                        action = GeofenceEnforcementService.ACTION_ZONE_ENTER
                        putExtra(GeofenceEnforcementService.EXTRA_ZONE_ID, zone.id)
                        putExtra(GeofenceEnforcementService.EXTRA_ZONE_NAME, zone.name)
                        putStringArrayListExtra(
                            GeofenceEnforcementService.EXTRA_BLOCKED_APPS,
                            ArrayList(zone.blockedApps)
                        )
                        putExtra(GeofenceEnforcementService.EXTRA_IS_TIME_SENSITIVE, zone.isTimeSensitive)
                        putExtra(GeofenceEnforcementService.EXTRA_START_TIME, zone.startTime)
                        putExtra(GeofenceEnforcementService.EXTRA_END_TIME, zone.endTime)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                for (geofence in triggered) {
                    val serviceIntent = Intent(context, GeofenceEnforcementService::class.java).apply {
                        action = GeofenceEnforcementService.ACTION_ZONE_EXIT
                        putExtra(GeofenceEnforcementService.EXTRA_ZONE_ID, geofence.requestId)
                    }
                    context.startService(serviceIntent)
                }
            }

            else -> Log.w(TAG, "Unhandled transition: $transition")
        }
    }
}
