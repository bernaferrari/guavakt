package dev.guavakt.util.concurrent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuturesTest {
    @Test
    fun settableFuture_setAndGet() {
        val f = SettableFuture.create<Int>()
        var listened = false
        f.addListener { listened = true }
        assertTrue(f.set(42))
        assertTrue(f.isDone())
        assertEquals(42, f.get())
        assertTrue(listened)
        assertEquals(7, Futures.getDone(Futures.immediateFuture(7)))
    }
}
