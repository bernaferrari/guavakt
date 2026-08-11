package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbstractFutureCancellationParityTest {
    @Test
    fun setFutureAfterCancellationStillCancelsDelegateWithInterruptFlag() {
        val outer = ProbeFuture()
        assertTrue(outer.cancel(true))
        val delegate = RecordingFuture()

        assertFalse(outer.link(delegate))

        assertTrue(delegate.isCancelled())
        assertTrue(delegate.interruptRequested)
        assertTrue(outer.interruptedCancellation())
    }

    @Test
    fun cancellationAfterSetFuturePropagatesAndInterruptsOuterTask() {
        val outer = ProbeFuture()
        val delegate = RecordingFuture()
        assertTrue(outer.link(delegate))

        assertTrue(outer.cancel(true))

        assertTrue(delegate.interruptRequested)
        assertTrue(outer.interruptCalls == 1)
    }

    @Test
    fun cancellationInheritedFromDelegateDoesNotPropagateInterruption() {
        val delegate = SettableFuture.create<Int>()
        delegate.cancel(true)
        val outer = ProbeFuture()

        assertTrue(outer.link(delegate))

        assertTrue(outer.isCancelled())
        assertFalse(outer.interruptedCancellation())
        assertTrue(outer.interruptCalls == 0)
    }

    private class ProbeFuture : AbstractFuture<Int>() {
        var interruptCalls = 0

        fun link(delegate: ListenableFuture<out Int>): Boolean = setAsync(delegate)
        fun interruptedCancellation(): Boolean = wasInterrupted()

        override fun interruptTask() {
            interruptCalls++
        }
    }

    private class RecordingFuture : ListenableFuture<Int> {
        var interruptRequested = false
        private var cancelled = false
        private val listeners = ArrayList<() -> Unit>()

        override fun isDone(): Boolean = cancelled
        override fun isCancelled(): Boolean = cancelled

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            if (cancelled) return false
            cancelled = true
            interruptRequested = mayInterruptIfRunning
            listeners.toList().forEach { it() }
            return true
        }

        override fun get(): Int = throw CancellationException("cancelled")

        override fun addListener(listener: () -> Unit) {
            if (cancelled) listener() else listeners += listener
        }
    }
}
