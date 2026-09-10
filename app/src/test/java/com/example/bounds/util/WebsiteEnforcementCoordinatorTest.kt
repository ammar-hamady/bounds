package com.example.bounds.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteEnforcementCoordinatorTest {

    @Test
    fun `enter grace activate exit follows one zone lifecycle`() {
        val coordinator = WebsiteEnforcementCoordinator()

        assertEquals(
            WebsiteEnforcementCoordinator.Transition.ENTERED,
            coordinator.enter("home", listOf("Example.com"))
        )
        assertFalse(coordinator.isActive())
        assertEquals(
            WebsiteEnforcementCoordinator.Transition.ACTIVATED,
            coordinator.activate("home")
        )
        assertTrue(coordinator.isActive())
        assertEquals(
            WebsiteEnforcementCoordinator.Transition.EXITED,
            coordinator.exit("home")
        )
        assertFalse(coordinator.isActive())
    }

    @Test
    fun `replacement wins and stale exit cannot clear it`() {
        val coordinator = WebsiteEnforcementCoordinator()
        coordinator.enter("home", listOf("home.example"))
        coordinator.activate("home")

        assertEquals(
            WebsiteEnforcementCoordinator.Transition.REPLACED,
            coordinator.enter("work", listOf("work.example"))
        )
        assertFalse(coordinator.isActive())
        assertEquals(
            WebsiteEnforcementCoordinator.Transition.STALE_EXIT,
            coordinator.exit("home")
        )
        assertTrue(coordinator.isCurrent("work"))
        assertEquals(
            WebsiteEnforcementCoordinator.Transition.DUPLICATE,
            coordinator.enter("work", listOf("changed.example"))
        )
    }
}