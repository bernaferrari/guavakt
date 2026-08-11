package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Blocking `Future.get` is deliberately a JVM-only compatibility bridge. */
class FuturesBlockingWaitJvmTest {
    @Test
    fun settableFuture_get_waitsUntilCompletedFromAnotherPath() {
        val future = SettableFuture.create<Int>()
        platformSchedule(80) { future.set(42) }
        assertEquals(42, future.get())
        assertTrue(future.isDone())
    }

    @Test
    fun transformAsync_fromIncompleteInput_chainsWhenCompleted() {
        val input = SettableFuture.create<Int>()
        val output = Futures.transformAsync(input) { value ->
            Futures.immediateFuture(value * 10)
        }
        platformSchedule(50) { input.set(2) }
        assertEquals(20, output.get())
    }

    @Test
    fun withTimeout_failsWhenInputNeverCompletes() {
        val input = SettableFuture.create<String>()
        val timed = Futures.withTimeout(input, 1)
        val failure = assertFailsWith<ExecutionException> { timed.get() }
        assertTrue(failure.cause is TimeoutException)
        assertTrue(input.isCancelled())
    }
}
