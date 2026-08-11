package dev.guavakt.util.concurrent

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecutionThreadServiceTest {
    @Test
    fun run_completesAndTerminates() = runTest {
        var ran = false
        val s = object : AbstractExecutionThreadService() {
            override fun run() { ran = true }
        }
        s.startAsync()
        s.awaitTerminatedSuspend()
        assertTrue(ran)
        assertEquals(Service.State.TERMINATED, s.state())
    }
}
