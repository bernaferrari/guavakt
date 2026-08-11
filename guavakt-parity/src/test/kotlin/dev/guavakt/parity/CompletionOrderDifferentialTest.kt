package dev.guavakt.parity

import com.google.common.util.concurrent.Futures as GuavaFutures
import com.google.common.util.concurrent.SettableFuture as GuavaSettableFuture
import dev.guavakt.util.concurrent.Futures
import dev.guavakt.util.concurrent.SettableFuture
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionOrderDifferentialTest {
    @Test
    fun cancelledSlotIsSkippedAndUnusedInputIsCancelledLikeGuava() {
        assertEquals(guavaCancelledSlotTrace(), guavaKtCancelledSlotTrace())
    }

    @Test
    fun cancellingEveryOutputCancelsEveryPendingInputLikeGuava() {
        assertEquals(guavaCancelAllTrace(), guavaKtCancelAllTrace())
    }

    private fun guavaCancelledSlotTrace(): List<Any?> {
        val first = GuavaSettableFuture.create<String>()
        val unused = GuavaSettableFuture.create<String>()
        val ordered = GuavaFutures.inCompletionOrder(listOf(first, unused))
        val cancelled = ordered[0].cancel(false)
        val set = first.set("first")
        return listOf(cancelled, set, ordered[1].get(), unused.isCancelled)
    }

    private fun guavaKtCancelledSlotTrace(): List<Any?> {
        val first = SettableFuture.create<String>()
        val unused = SettableFuture.create<String>()
        val ordered = Futures.inCompletionOrder(listOf(first, unused))
        val cancelled = ordered[0].cancel(false)
        val set = first.set("first")
        return listOf(cancelled, set, ordered[1].get(), unused.isCancelled())
    }

    private fun guavaCancelAllTrace(): List<Any?> {
        val first = GuavaSettableFuture.create<String>()
        val second = GuavaSettableFuture.create<String>()
        val ordered = GuavaFutures.inCompletionOrder(listOf(first, second))
        return listOf(
            ordered[0].cancel(true),
            ordered[1].cancel(false),
            first.isCancelled,
            second.isCancelled,
        )
    }

    private fun guavaKtCancelAllTrace(): List<Any?> {
        val first = SettableFuture.create<String>()
        val second = SettableFuture.create<String>()
        val ordered = Futures.inCompletionOrder(listOf(first, second))
        return listOf(
            ordered[0].cancel(true),
            ordered[1].cancel(false),
            first.isCancelled(),
            second.isCancelled(),
        )
    }
}
