package com.bernaferrari.guavakt.collect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImmutableListTest {
    @Test
    fun ofAndCopyOf() {
        assertTrue(ImmutableList.of<Int>().isEmpty())
        assertEquals(listOf(1, 2, 3), ImmutableList.of(1, 2, 3).toList())
        assertEquals(listOf(1, 2), ImmutableList.copyOf(listOf(1, 2)).toList())
    }

    @Test
    fun subListAndReverse() {
        val list = ImmutableList.of(1, 2, 3, 4)
        assertEquals(listOf(2, 3), list.subList(1, 3).toList())
        assertEquals(listOf(4, 3, 2, 1), list.reverse().toList())
    }

    @Test
    fun builder() {
        val list = ImmutableList.builder<String>().add("a").addAll(listOf("b", "c")).build()
        assertEquals(listOf("a", "b", "c"), list.toList())
    }

    @Test
    fun sortedCopyOf() {
        assertEquals(listOf(1, 2, 3), ImmutableList.sortedCopyOf(listOf(3, 1, 2)).toList())
    }

    @Test
    fun indexChecks() {
        val list = ImmutableList.of("x")
        assertFailsWith<IndexOutOfBoundsException> { list[-1] }
        assertFailsWith<IndexOutOfBoundsException> { list[1] }
    }
}
