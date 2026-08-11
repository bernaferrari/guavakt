package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncFunctionCallableTest {
    @Test
    fun asyncFunctionOverloadDelegatesToItsReturnedFuture() {
        val transformed = Futures.transformAsync(
            Futures.immediateFuture(4),
            AsyncFunction<Int, String> { Futures.immediateFuture("value=${it * 2}") },
        )

        assertEquals("value=8", transformed.get())
    }

    @Test
    fun asyncCallableDelegatesAndCancelledWorkIsNeverStarted() {
        val executor = QueuedExecutor()
        var calls = 0
        val result = Futures.submitAsync(
            AsyncCallable {
                calls++
                Futures.immediateFuture("done")
            },
            executor,
        )

        assertTrue(result.cancel(false))
        executor.runQueued()
        assertEquals(0, calls)

        val completed = Futures.submitAsync(
            AsyncCallable {
                calls++
                Futures.immediateFuture("done")
            },
            executor,
        )
        executor.runQueued()

        assertEquals("done", completed.get())
        assertEquals(1, calls)
        assertFalse(completed.isCancelled())
    }

    private class QueuedExecutor : AbstractListeningExecutorService() {
        private val tasks = ArrayDeque<() -> Unit>()

        override fun execute(command: () -> Unit) {
            tasks += command
        }

        fun runQueued() {
            while (tasks.isNotEmpty()) tasks.removeFirst()()
        }
    }
}
