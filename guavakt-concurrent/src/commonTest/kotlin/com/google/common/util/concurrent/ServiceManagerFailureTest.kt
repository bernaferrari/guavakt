package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServiceManagerFailureTest {
    private class FailService : AbstractService() {
        override fun doStart() {
            notifyFailed(IllegalStateException("boom"))
        }
        override fun doStop() {}
    }

    @Test
    fun awaitHealthy_throwsWhenServiceFailed() {
        val m = ServiceManager(listOf(FailService()))
        m.startAsync()
        assertFailsWith<IllegalStateException> { m.awaitHealthy() }
    }

    @Test
    fun failureListener_invoked() {
        var failed = false
        val m = ServiceManager(listOf(FailService()))
        m.addListener(object : ServiceManager.Listener() {
            override fun failure(service: Service) { failed = true }
        })
        m.startAsync()
        assertTrue(failed)
    }
}
