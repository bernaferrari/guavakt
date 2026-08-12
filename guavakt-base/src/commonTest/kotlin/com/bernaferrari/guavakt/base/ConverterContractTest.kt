package com.bernaferrari.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ConverterContractTest {
    @Test
    fun reverseIsCachedReciprocalAndIdentityIsShared() {
        val converter = Converter.from<Int, String>(Forward, Backward)
        val reverse = converter.reverse()

        assertSame(reverse, converter.reverse())
        assertSame(converter, reverse.reverse())
        assertEquals("n7", converter.convert(7))
        assertEquals(7, reverse.convert("n7"))

        val identity = Converter.identity<String>()
        assertSame(identity, identity.reverse())
        assertSame(converter, Converter.identity<Int>().andThen(converter))
    }

    @Test
    fun convertAllIsLazyAndReiterable() {
        val source = CountingIterable(listOf(1, 2, 3))
        val converted = Converter.from<Int, String>(Forward, Backward).convertAll(source)
        assertEquals(0, source.iteratorCalls)

        assertEquals(listOf("n1", "n2", "n3"), converted.toList())
        assertEquals(1, source.iteratorCalls)
        assertEquals(listOf("n1", "n2", "n3"), converted.toList())
        assertEquals(2, source.iteratorCalls)
    }

    @Test
    fun derivedConvertersKeepFunctionBasedValueSemanticsAndRejectNullResults() {
        val first = Converter.from<Int, String>(Forward, Backward)
        val second = Converter.from<Int, String>(Forward, Backward)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals(first.reverse(), second.reverse())
        assertEquals(first.andThen(first.reverse()), second.andThen(second.reverse()))

        val nullForward = object : Converter<String, String?>() {
            override fun doForward(a: String): String? = null
            override fun doBackward(b: String?): String = b.orEmpty()
        }
        assertFailsWith<NullPointerException> { nullForward.convert("x") }
    }

    private object Forward : Function<Int, String> {
        override fun apply(input: Int): String = "n$input"
        override fun toString(): String = "forward"
    }

    private object Backward : Function<String, Int> {
        override fun apply(input: String): Int = input[1].code - '0'.code
        override fun toString(): String = "backward"
    }

    private class CountingIterable(private val values: List<Int>) : Iterable<Int> {
        var iteratorCalls = 0
        override fun iterator(): Iterator<Int> {
            iteratorCalls++
            return values.iterator()
        }
    }
}
