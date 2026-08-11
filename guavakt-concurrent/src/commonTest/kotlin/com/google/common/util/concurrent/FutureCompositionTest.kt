package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FutureCompositionTest {
    @Test
    fun transform_applies_function() {
        val input = Futures.immediateFuture(21)
        val out = Futures.transform(input) { it * 2 }
        assertTrue(out.isDone())
        assertEquals(42, out.get())
    }

    @Test
    fun catching_recovers() {
        val failed = Futures.immediateFailedFuture<Int>(IllegalStateException("boom"))
        val recovered = Futures.catching(failed, IllegalStateException::class) { -1 }
        assertEquals(-1, recovered.get())
    }

    @Test
    fun allAsList_aggregates() {
        val f1 = Futures.immediateFuture(1)
        val f2 = Futures.immediateFuture(2)
        val all = Futures.allAsList(listOf(f1, f2))
        assertEquals(listOf(1, 2), all.get())
    }

    @Test
    fun closingFuture_runs_closeables() {
        var closed = false
        val base = Futures.immediateFuture("ok")
        val cf = ClosingFuture.from(base)
        cf.eventuallyWillClose(AutoCloseable { closed = true })
        assertEquals("ok", cf.get())
        // listener may run sync on settable already done — ensure closed
        // force by finishing
        cf.finishToFuture()
        // close runs on completion listener; already done so should have closed
        assertTrue(closed)
    }
}
