package com.example.bounds.data

import com.example.bounds.model.Zone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveEnforcementMutationPolicyTest {
    private val active = Zone(
        id = "active",
        name = "Work",
        isEnabled = true,
        blockedApps = listOf("Instagram"),
        blockedDomains = listOf("example.com"),
        latitude = 1.0,
        longitude = 2.0,
        radiusMeters = 100
    )
    private val inactive = Zone(id = "inactive", name = "Home")
    private val current = listOf(active, inactive)

    @Test fun `active zone cannot be disabled`() =
        assertFalse(permits(active.copy(isEnabled = false)))

    @Test fun `active zone definition cannot be edited`() =
        assertFalse(permits(active.copy(radiusMeters = 200)))

    @Test fun `active app blocklist cannot be edited`() =
        assertFalse(permits(active.copy(blockedApps = emptyList())))

    @Test fun `active website blocklist cannot be edited`() =
        assertFalse(permits(active.copy(blockedDomains = emptyList())))

    @Test fun `active zone cannot be deleted`() =
        assertFalse(
            ActiveEnforcementMutationPolicy.permitsZoneMutation(
                current,
                listOf(inactive),
                active.id
            )
        )

    @Test fun `inactive zone remains editable during enforcement`() =
        assertTrue(
            ActiveEnforcementMutationPolicy.permitsZoneMutation(
                current,
                listOf(active, inactive.copy(name = "Changed")),
                active.id
            )
        )

    @Test fun `same policy protects grace period because protection is keyed by active id`() =
        assertFalse(permits(active.copy(name = "Stale editor save")))

    @Test fun `normal zone editing returns when enforcement ends`() =
        assertTrue(
            ActiveEnforcementMutationPolicy.permitsZoneMutation(
                current,
                listOf(active.copy(isEnabled = false), inactive),
                null
            )
        )

    @Test fun `unknown protected zone fails closed`() =
        assertFalse(
            ActiveEnforcementMutationPolicy.permitsZoneMutation(current, current, "missing")
        )

    @Test fun `enforcement settings are locked during grace or blocking`() =
        assertFalse(
            ActiveEnforcementMutationPolicy.permitsEnforcementSettingMutation(
                isEnforcementActive = true
            )
        )

    @Test fun `enforcement settings unlock after leaving zone`() =
        assertTrue(
            ActiveEnforcementMutationPolicy.permitsEnforcementSettingMutation(
                isEnforcementActive = false
            )
        )

    @Test fun `guard rejects stale zone save after enforcement starts`() {
        val guard = ActiveEnforcementMutationGuard()
        guard.updateProtectedZone(active.id) {}
        var committed = false

        val allowed = guard.commitZoneMutation(
            current,
            listOf(active.copy(name = "Stale"), inactive)
        ) { committed = true }

        assertFalse(allowed)
        assertFalse(committed)
    }

    @Test fun `guard restores inactive zone edits after enforcement ends`() {
        val guard = ActiveEnforcementMutationGuard()
        guard.updateProtectedZone(active.id) {}
        guard.updateProtectedZone(null) {}

        assertTrue(
            guard.commitZoneMutation(
                current,
                listOf(active, inactive.copy(name = "Changed"))
            ) {}
        )
    }

    @Test fun `guard rejects enforcement setting mutation while active`() {
        val guard = ActiveEnforcementMutationGuard()
        guard.updateProtectedZone(active.id) {}
        var committed = false

        assertFalse(guard.commitEnforcementSettingMutation { committed = true })
        assertFalse(committed)
    }

    private fun permits(replacement: Zone): Boolean =
        ActiveEnforcementMutationPolicy.permitsZoneMutation(
            current,
            listOf(replacement, inactive),
            active.id
        )
}