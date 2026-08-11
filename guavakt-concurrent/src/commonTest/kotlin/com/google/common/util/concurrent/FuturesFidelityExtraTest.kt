package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuturesFidelityExtraTest {
    @Test
    fun transform_withExecutor_appliesFunction() {
        val f = Futures.immediateFuture(2)
        val t = Futures.transform(f) { it * 3 }
        assertEquals(6, t.get())
    }

    @Test
    fun allAsList_collects() {
        val a = Futures.immediateFuture(1)
        val b = Futures.immediateFuture(2)
        val all = Futures.allAsList(listOf(a, b))
        assertEquals(listOf(1, 2), all.get())
    }

    @Test
    fun catching_recovers() {
        val failed = Futures.immediateFailedFuture<Int>(IllegalStateException("x"))
        val recovered = Futures.catching(failed, IllegalStateException::class) { 99 }
        assertEquals(99, recovered.get())
    }

    @Test
    fun settable_setException_isDone() {
        val f = SettableFuture.create<Int>()
        assertTrue(f.setException(RuntimeException("e")))
        assertTrue(f.isDone())
    }
}
