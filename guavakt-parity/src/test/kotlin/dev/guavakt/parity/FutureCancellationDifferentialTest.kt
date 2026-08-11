package dev.guavakt.parity

import com.google.common.util.concurrent.GuavaFutureCancellationHarness
import dev.guavakt.util.concurrent.AbstractFuture
import dev.guavakt.util.concurrent.CancellationException
import dev.guavakt.util.concurrent.ListenableFuture
import dev.guavakt.util.concurrent.SettableFuture
import kotlin.test.Test
import kotlin.test.assertEquals

class FutureCancellationDifferentialTest {
    @Test
    fun cancelThenSetFutureMatchesGuava() {
        assertEquals(GuavaFutureCancellationHarness.cancelThenSetFutureTrace(), cancelThenSetFutureTrace())
    }

    @Test
    fun setFutureThenCancelMatchesGuava() {
        assertEquals(GuavaFutureCancellationHarness.setFutureThenCancelTrace(), setFutureThenCancelTrace())
    }

    @Test
    fun cancelledDelegateDoesNotPropagateInterruptBit() {
        assertEquals(GuavaFutureCancellationHarness.cancelledDelegateTrace(), cancelledDelegateTrace())
    }

    private fun cancelThenSetFutureTrace(): List<String> {
        val outer = ProbeFuture()
        val delegate = RecordingFuture()
        outer.cancel(true)
        val accepted = outer.link(delegate)
        return listOf(
            "accepted:$accepted",
            "delegate-cancelled:${delegate.isCancelled()}",
            "delegate-interrupt:${delegate.interruptRequested}",
            "outer-interrupt:${outer.interruptedCancellation()}",
            "outer-interrupt-calls:${outer.interruptCalls}",
        )
    }

    private fun setFutureThenCancelTrace(): List<String> {
        val outer = ProbeFuture()
        val delegate = RecordingFuture()
        val accepted = outer.link(delegate)
        outer.cancel(true)
        return listOf(
            "accepted:$accepted",
            "delegate-cancelled:${delegate.isCancelled()}",
            "delegate-interrupt:${delegate.interruptRequested}",
            "outer-interrupt:${outer.interruptedCancellation()}",
            "outer-interrupt-calls:${outer.interruptCalls}",
        )
    }

    private fun cancelledDelegateTrace(): List<String> {
        val delegate = SettableFuture.create<Int>()
        delegate.cancel(true)
        val outer = ProbeFuture()
        val accepted = outer.link(delegate)
        return listOf(
            "accepted:$accepted",
            "outer-cancelled:${outer.isCancelled()}",
            "outer-interrupt:${outer.interruptedCancellation()}",
            "outer-interrupt-calls:${outer.interruptCalls}",
        )
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
            listeners.clear()
            return true
        }
        override fun get(): Int = throw CancellationException("cancelled")
        override fun addListener(listener: () -> Unit) {
            if (cancelled) listener() else listeners += listener
        }
    }
}
