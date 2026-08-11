package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbstractFutureDepthTest {
    @Test fun settable_set_get_listener() {
        var heard = false
        val f = SettableFuture.create<Int>()
        f.addListener { heard = true }
        assertTrue(f.set(42))
        assertFalse(f.set(99))
        assertEquals(42, f.get())
        assertTrue(heard)
        assertTrue(f.isDone())
    }

    @Test fun settable_setException() {
        val f = SettableFuture.create<Int>()
        f.setException(IllegalStateException("boom"))
        val ex = assertFailsWith<ExecutionException> { f.get() }
        assertTrue(ex.cause is IllegalStateException)
    }

    @Test fun settable_cancel() {
        val f = SettableFuture.create<Int>()
        assertTrue(f.cancel(false))
        assertTrue(f.isCancelled())
        assertFailsWith<CancellationException> { f.get() }
    }

    @Test fun setFuture_propagates() {
        val inner = SettableFuture.create<String>()
        val outer = SettableFuture.create<String>()
        assertTrue(outer.setFuture(inner))
        inner.set("ok")
        assertEquals("ok", outer.get())
    }

    @Test fun setFuture_propagatesFailure() {
        val inner = SettableFuture.create<String>()
        val outer = SettableFuture.create<String>()
        outer.setFuture(inner)
        inner.setException(RuntimeException("x"))
        val ex = assertFailsWith<ExecutionException> { outer.get() }
        assertEquals("x", ex.cause?.message)
    }

    @Test fun immediate_cancelled() {
        val f = Futures.immediateCancelledFuture<Int>()
        assertTrue(f.isCancelled())
    }

    @Test fun transform_and_async() {
        val inF = Futures.immediateFuture(2)
        val t = Futures.transform(inF) { it * 3 }
        assertEquals(6, t.get())
        val a = Futures.transformAsync(Futures.immediateFuture(4)) { v ->
            Futures.immediateFuture(v + 1)
        }
        assertEquals(5, a.get())
    }

    @Test fun catching_recovers() {
        val failed = Futures.immediateFailedFuture<Int>(IllegalArgumentException("n"))
        val recovered = Futures.catching(failed, IllegalArgumentException::class) { -1 }
        assertEquals(-1, recovered.get())
    }

    @Test fun catchingAsync_recovers() {
        val failed = Futures.immediateFailedFuture<Int>(IllegalStateException("n"))
        val recovered = Futures.catchingAsync(failed, IllegalStateException::class) {
            Futures.immediateFuture(7)
        }
        assertEquals(7, recovered.get())
    }

    @Test fun allAsList_and_successfulAsList() {
        val all = Futures.allAsList(
            Futures.immediateFuture(1),
            Futures.immediateFuture(2),
        )
        assertEquals(listOf(1, 2), all.get())
        val mixed = Futures.successfulAsList(
            Futures.immediateFuture(1),
            Futures.immediateFailedFuture<Int>(RuntimeException("x")),
        )
        val result = mixed.get()
        assertEquals(1, result[0])
        assertEquals(null, result[1])
    }

    @Test fun getDone_and_getUnchecked() {
        val f = Futures.immediateFuture(9)
        assertEquals(9, Futures.getDone(f))
        assertEquals(9, Futures.getUnchecked(f))
        val bad = Futures.immediateFailedFuture<Int>(IllegalArgumentException("e"))
        assertFailsWith<UncheckedExecutionException> { Futures.getUnchecked(bad) }
    }

    @Test fun nonCancellationPropagating_and_whenAllComplete() {
        val inner = SettableFuture.create<Int>()
        val outer = Futures.nonCancellationPropagating(inner)
        inner.set(3)
        assertEquals(3, outer.get())
        val w = Futures.whenAllComplete(listOf(Futures.immediateFuture(1), Futures.immediateFuture(2)))
        assertEquals(2, w.get().size)
    }

    @Test fun inCompletionOrder_usesInputCompletionOrder() {
        val ordered = Futures.inCompletionOrder(
            listOf(Futures.immediateFuture("a"), Futures.immediateFuture("b")),
        )
        assertEquals("a", ordered[0].get())
        assertEquals("b", ordered[1].get())
    }

    @Test fun inCompletionOrder_cancellationSkipsCancelledSlotAndCancelsUnusedInput() {
        val first = SettableFuture.create<String>()
        val unused = SettableFuture.create<String>()
        val ordered = Futures.inCompletionOrder(listOf(first, unused))

        assertTrue(ordered[0].cancel(false))
        assertTrue(first.set("first"))

        assertEquals("first", ordered[1].get())
        assertTrue(unused.isCancelled())
    }

    @Test fun inCompletionOrder_cancellingEveryOutputCancelsInputs() {
        val first = SettableFuture.create<String>()
        val second = SettableFuture.create<String>()
        val ordered = Futures.inCompletionOrder(listOf(first, second))

        assertTrue(ordered[0].cancel(true))
        assertTrue(ordered[1].cancel(false))

        assertTrue(first.isCancelled())
        assertTrue(second.isCancelled())
    }
}
