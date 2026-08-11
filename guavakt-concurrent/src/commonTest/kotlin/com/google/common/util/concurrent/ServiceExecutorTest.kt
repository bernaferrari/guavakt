package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceExecutorTest {
    @Test
    fun abstractIdleService_lifecycle() {
        var started = false
        var stopped = false
        val service = object : AbstractIdleService() {
            override fun startUp() { started = true }
            override fun shutDown() { stopped = true }
        }
        service.startAsync()
        assertTrue(service.isRunning())
        assertEquals(Service.State.RUNNING, service.state())
        assertTrue(started)
        service.stopAsync()
        assertEquals(Service.State.TERMINATED, service.state())
        assertTrue(stopped)
    }

    @Test
    fun directExecutor_submitCompletes() {
        val exec = MoreExecutors.directExecutor()
        val f = exec.submit { 21 * 2 }
        assertTrue(f.isDone())
        assertEquals(42, f.get())
    }
}
