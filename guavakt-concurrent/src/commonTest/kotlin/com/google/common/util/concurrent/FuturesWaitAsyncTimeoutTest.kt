package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FuturesWaitAsyncTimeoutTest {
    @Test
    fun settableFuture_get_afterSetException() {
        val f = SettableFuture.create<Int>()
        f.setException(IllegalStateException("boom"))
        val ex = assertFailsWith<ExecutionException> { f.get() }
        assertTrue(ex.cause is IllegalStateException)
    }

    @Test
    fun transformAsync_propagatesFailure() {
        val input = Futures.immediateFailedFuture<Int>(RuntimeException("x"))
        val out = Futures.transformAsync(input) { Futures.immediateFuture(it) }
        assertFailsWith<ExecutionException> { out.get() }
    }

    @Test
    fun withTimeout_completesWhenInputFast() {
        val input = Futures.immediateFuture("ok")
        val timed = Futures.withTimeout(input, 60_000)
        assertEquals("ok", timed.get())
    }

    @Test
    fun withTimeout_deliversAsynchronouslyAndCancelsDelegate() = runTest {
        val input = SettableFuture.create<String>()
        val timed = Futures.withTimeout(input, 1)
        assertTrue(!timed.isDone(), "a positive timeout must not fire inline")
        assertFailsWith<TimeoutException> { timed.await() }
        assertTrue(input.isCancelled())
    }

    @Test
    fun portableTimedGetRefusesPendingBlockingFuture() {
        if (platformSupportsBlockingWait()) return
        assertFailsWith<UnsupportedOperationException> {
            Uninterruptibles.getUninterruptibly(SettableFuture.create<Int>(), 1)
        }
    }
}
