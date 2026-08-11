package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionalTest {
    @Test
    fun presentAndAbsent() {
        val p = Optional.of("hi")
        assertTrue(p.isPresent())
        assertFalse(p.isAbsent())
        assertEquals("hi", p.get())
        assertEquals("hi", p.or("x"))
        assertEquals(setOf("hi"), p.asSet())

        val a = Optional.absent<String>()
        assertFalse(a.isPresent())
        assertTrue(a.isAbsent())
        assertEquals("x", a.or("x"))
        assertEquals(null, a.orNull())
    }

    @Test
    fun transformAndPresentInstances() {
        val t = Optional.of(2).transform(Function { it * 3 })
        assertEquals(6, t.get())
        val present = Optional.presentInstances(
            listOf(Optional.of(1), Optional.absent(), Optional.of(2)),
        ).toList()
        assertEquals(listOf(1, 2), present)
    }
}
