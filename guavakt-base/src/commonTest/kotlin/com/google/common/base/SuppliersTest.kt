package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals

class SuppliersTest {
    @Test
    fun memoize_once() {
        var n = 0
        val s = Suppliers.memoize { n++; 7 }
        assertEquals(7, s.get())
        assertEquals(7, s.get())
        assertEquals(1, n)
    }

    @Test
    fun memoizeWithExpiration_refreshes() {
        val t = object : Ticker() {
            var n = 0L
            override fun read() = n
        }
        var loads = 0
        val s = Suppliers.memoizeWithExpiration({ loads++; loads }, 10L, t)
        assertEquals(1, s.get())
        t.n = 5
        assertEquals(1, s.get())
        t.n = 10
        assertEquals(2, s.get())
    }

    @Test
    fun compose_and_ofInstance() {
        assertEquals(3, Suppliers.compose({ x: Int -> x + 1 }, Suppliers.ofInstance(2)).get())
    }
}
