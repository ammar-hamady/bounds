package com.example.bounds.data

import com.example.bounds.model.Zone

/**
 * Synchronizes enforcement transitions with the final in-DataStore mutation
 * callback. This closes the activation/write race without holding a monitor
 * across a suspending DataStore operation.
 */
class ActiveEnforcementMutationGuard {
    private val lock = Any()
    private var protectedZoneId: String? = null

    fun updateProtectedZone(zoneId: String?, updateEnforcementState: () -> Unit) {
        synchronized(lock) {
            protectedZoneId = zoneId
            updateEnforcementState()
        }
    }

    fun commitZoneMutation(
        currentZones: List<Zone>,
        proposedZones: List<Zone>,
        commit: () -> Unit
    ): Boolean = synchronized(lock) {
        if (!ActiveEnforcementMutationPolicy.permitsZoneMutation(
                currentZones,
                proposedZones,
                protectedZoneId
            )
        ) {
            return@synchronized false
        }
        commit()
        true
    }

    fun commitEnforcementSettingMutation(commit: () -> Unit): Boolean = synchronized(lock) {
        if (!ActiveEnforcementMutationPolicy.permitsEnforcementSettingMutation(
                protectedZoneId != null
            )
        ) {
            return@synchronized false
        }
        commit()
        true
    }

    fun protectedZoneId(): String? = synchronized(lock) { protectedZoneId }
}