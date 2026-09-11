package com.example.bounds.data

import com.example.bounds.model.Zone

/** Central policy for every in-app mutation that could weaken active enforcement. */
object ActiveEnforcementMutationPolicy {
    fun permitsZoneMutation(
        currentZones: List<Zone>,
        proposedZones: List<Zone>,
        protectedZoneId: String?
    ): Boolean {
        if (protectedZoneId == null) return true
        val current = currentZones.firstOrNull { it.id == protectedZoneId } ?: return false
        val proposed = proposedZones.firstOrNull { it.id == protectedZoneId } ?: return false
        return current == proposed
    }

    fun permitsEnforcementSettingMutation(isEnforcementActive: Boolean): Boolean =
        !isEnforcementActive
}