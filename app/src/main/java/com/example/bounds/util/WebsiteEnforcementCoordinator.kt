package com.example.bounds.util

/**
 * Pure state holder for the one-zone website policy. The Android service uses
 * the same rules: duplicate enters are ignored, a new zone replaces the old
 * policy, and stale exits cannot clear the winning zone.
 */
class WebsiteEnforcementCoordinator {

    enum class Transition {
        ENTERED,
        REPLACED,
        DUPLICATE,
        ACTIVATED,
        EXITED,
        STALE_EXIT
    }

    private var currentZoneId: String? = null
    private var currentDomains: List<String> = emptyList()
    private var active = false

    fun enter(zoneId: String, domains: List<String>): Transition {
        if (currentZoneId == zoneId) return Transition.DUPLICATE
        val replaced = currentZoneId != null
        currentZoneId = zoneId
        currentDomains = DomainBlocklist.canonicalizeAll(domains)
        active = false
        return if (replaced) Transition.REPLACED else Transition.ENTERED
    }

    fun activate(zoneId: String): Transition? {
        if (currentZoneId != zoneId) return null
        active = true
        return Transition.ACTIVATED
    }

    fun exit(zoneId: String?): Transition {
        if (zoneId != null && zoneId != currentZoneId) return Transition.STALE_EXIT
        currentZoneId = null
        currentDomains = emptyList()
        active = false
        return Transition.EXITED
    }

    fun isCurrent(zoneId: String): Boolean = currentZoneId == zoneId
    fun currentDomains(): List<String> = currentDomains
    fun isActive(): Boolean = active
}