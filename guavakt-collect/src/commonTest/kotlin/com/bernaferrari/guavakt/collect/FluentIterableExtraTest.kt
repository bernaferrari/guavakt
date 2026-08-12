package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Joiner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FluentIterableExtraTest {
    @Test
    fun transformFilterJoin() {
        val f = FluentIterable.from(listOf(1, 2, 3, 4))
            .filter { it % 2 == 0 }
            .transform { it * 10 }
        assertEquals(listOf(20, 40), f.toList())
        assertEquals("20,40", f.join(Joiner.on(",")))
        assertEquals(mapOf(20 to "20", 40 to "40"), f.toMap { it.toString() })
    }

    @Test
    fun uniqueIndex() {
        val m = FluentIterable.from(listOf("a", "bb")).uniqueIndex { it.length }
        assertEquals("a", m[1])
        assertEquals("bb", m[2])
    }
}
