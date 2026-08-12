package com.bernaferrari.guavakt.parity

import com.google.common.base.Converter as GuavaConverter
import com.google.common.base.Function as GuavaFunction
import com.bernaferrari.guavakt.base.Converter as GuavaKtConverter
import com.bernaferrari.guavakt.base.Function as GuavaKtFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ConverterDifferentialTest {
    @Test
    fun conversionReverseIdentityCompositionAndLazyBulkValuesMatchGuava() {
        val guava = GuavaConverter.from(GuavaForward, GuavaBackward)
        val kotlin = GuavaKtConverter.from(KotlinForward, KotlinBackward)

        assertEquals(guava.convert(7), kotlin.convert(7))
        assertEquals(guava.reverse().convert("n7"), kotlin.reverse().convert("n7"))
        assertSame(guava.reverse(), guava.reverse())
        assertSame(kotlin.reverse(), kotlin.reverse())
        assertSame(guava, guava.reverse().reverse())
        assertSame(kotlin, kotlin.reverse().reverse())
        assertEquals(guava.convertAll(listOf(1, 2, 3)).toList(), kotlin.convertAll(listOf(1, 2, 3)).toList())
        assertEquals(guava.andThen(guava.reverse()).convert(7), kotlin.andThen(kotlin.reverse()).convert(7))

        val guavaIdentity = GuavaConverter.identity<String>()
        val kotlinIdentity = GuavaKtConverter.identity<String>()
        assertSame(guavaIdentity, guavaIdentity.reverse())
        assertSame(kotlinIdentity, kotlinIdentity.reverse())
    }

    @Test
    fun functionBasedAndDerivedValueSemanticsMatchGuava() {
        val guavaFirst = GuavaConverter.from(GuavaForward, GuavaBackward)
        val guavaSecond = GuavaConverter.from(GuavaForward, GuavaBackward)
        val kotlinFirst = GuavaKtConverter.from(KotlinForward, KotlinBackward)
        val kotlinSecond = GuavaKtConverter.from(KotlinForward, KotlinBackward)

        assertEquals(guavaFirst == guavaSecond, kotlinFirst == kotlinSecond)
        assertEquals(guavaFirst.reverse() == guavaSecond.reverse(), kotlinFirst.reverse() == kotlinSecond.reverse())
        assertEquals(
            guavaFirst.andThen(guavaFirst.reverse()) == guavaSecond.andThen(guavaSecond.reverse()),
            kotlinFirst.andThen(kotlinFirst.reverse()) == kotlinSecond.andThen(kotlinSecond.reverse()),
        )
    }

    private object GuavaForward : GuavaFunction<Int, String> {
        override fun apply(input: Int): String = "n$input"
        override fun toString(): String = "forward"
    }

    private object GuavaBackward : GuavaFunction<String, Int> {
        override fun apply(input: String): Int = input[1].code - '0'.code
        override fun toString(): String = "backward"
    }

    private object KotlinForward : GuavaKtFunction<Int, String> {
        override fun apply(input: Int): String = "n$input"
        override fun toString(): String = "forward"
    }

    private object KotlinBackward : GuavaKtFunction<String, Int> {
        override fun apply(input: String): Int = input[1].code - '0'.code
        override fun toString(): String = "backward"
    }
}
