package com.example.bounds.util

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.bounds.model.Zone
import com.example.bounds.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

private const val TAG = "BoundsGeofenceMgr"

/**
 * Thin wrapper around the Play Services GeofencingClient.
 *
 * Call [syncGeofences] whenever the zone list or a zone's enabled-state changes.
 * Silently skips registration when the required permissions are absent so the
 * rest of the app continues to function (zones shown as inactive in the UI).
 */
object BoundsGeofenceManager {

    private var geofencingClient: GeofencingClient? = null

    private fun client(ctx: Context): GeofencingClient {
        if (geofencingClient == null) {
            geofencingClient = LocationServices.getGeofencingClient(ctx.applicationContext)
        }
        return geofencingClient!!
    }

    private fun pendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx.applicationContext, GeofenceBroadcastReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(ctx.applicationContext, 0, intent, flags)
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    fun hasFineLocation(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /** Background location is only required on Android 10 (Q) and above. */
    fun hasBackgroundLocation(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Registers exactly the enabled zones that have valid coordinates.
     * Removes any previously-registered geofences first for a clean slate.
     */
    fun syncGeofences(ctx: Context, zones: List<Zone>) {
        if (!hasFineLocation(ctx)) {
            Log.w(TAG, "Fine location not granted — skipping geofence sync")
            return
        }
        val pi = pendingIntent(ctx)
        client(ctx).removeGeofences(pi).addOnCompleteListener {
            val active = zones.filter {
                it.isEnabled && it.latitude != 0.0 && it.longitude != 0.0
            }
            if (active.isEmpty()) return@addOnCompleteListener

            val geofences = active.map { z ->
                Geofence.Builder()
                    .setRequestId(z.id)
                    .setCircularRegion(
                        z.latitude,
                        z.longitude,
                        z.radiusMeters.toFloat().coerceAtLeast(50f)
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            try {
                client(ctx).addGeofences(request, pi)
                    .addOnSuccessListener { Log.i(TAG, "Registered ${geofences.size} geofence(s)") }
                    .addOnFailureListener { e -> Log.e(TAG, "Register failed: ${e.message}") }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: ${e.message}")
            }
        }
    }

    fun removeAll(ctx: Context) {
        client(ctx).removeGeofences(pendingIntent(ctx))
            .addOnSuccessListener { Log.i(TAG, "All geofences removed") }
    }
}
