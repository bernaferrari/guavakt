package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceManagerTest {
    private class S : AbstractIdleService() {
        var up = false
        override fun startUp() { up = true }
        override fun shutDown() { up = false }
    }

    @Test
    fun startAndStop_allServices() {
        val a = S(); val b = S()
        val m = ServiceManager(listOf(a, b))
        m.startAsync()
        m.awaitHealthy()
        assertTrue(m.isHealthy())
        assertTrue(a.up && b.up)
        m.stopAsync()
        m.awaitStopped()
        assertEquals(Service.State.TERMINATED, a.state())
    }
}
