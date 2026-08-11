package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class GuavaFuturesFidelityTest {
    @Test fun transform_chains() {
        val f = Futures.transform(Futures.immediateFuture(10)) { it + 1 }
        val g = Futures.transform(f) { it * 2 }
        assertEquals(22, g.get())
    }

    @Test fun catching_only_matching_type() {
        val failed = Futures.immediateFailedFuture<Int>(IllegalArgumentException("bad"))
        val recovered = Futures.catching(failed, IllegalArgumentException::class) { 0 }
        assertEquals(0, recovered.get())
        val failed2 = Futures.immediateFailedFuture<Int>(IllegalStateException("x"))
        val notRecovered = Futures.catching(failed2, IllegalArgumentException::class) { 0 }
        assertFails { notRecovered.get() }
    }

    @Test fun allAsList_order_preserved() {
        val all = Futures.allAsList(
            listOf(
                Futures.immediateFuture("a"),
                Futures.immediateFuture("b"),
                Futures.immediateFuture("c"),
            ),
        )
        assertEquals(listOf("a", "b", "c"), all.get())
    }

    @Test fun settable_listeners_fire_once_done() {
        val f = SettableFuture.create<Int>()
        var n = 0
        f.addListener { n++ }
        assertTrue(f.set(1))
        assertEquals(1, n)
        f.addListener { n++ } // late listener runs immediately
        assertEquals(2, n)
    }
}
