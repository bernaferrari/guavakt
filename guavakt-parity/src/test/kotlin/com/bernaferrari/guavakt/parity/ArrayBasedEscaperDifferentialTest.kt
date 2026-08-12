package com.bernaferrari.guavakt.parity

import com.google.common.escape.ArrayBasedCharEscaper as GuavaArrayBasedCharEscaper
import com.google.common.escape.ArrayBasedUnicodeEscaper as GuavaArrayBasedUnicodeEscaper
import com.google.common.escape.Escapers as GuavaEscapers
import com.bernaferrari.guavakt.escape.ArrayBasedCharEscaper as GuavaKtArrayBasedCharEscaper
import com.bernaferrari.guavakt.escape.ArrayBasedUnicodeEscaper as GuavaKtArrayBasedUnicodeEscaper
import com.bernaferrari.guavakt.escape.Escapers as GuavaKtEscapers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArrayBasedEscaperDifferentialTest {
    @Test
    fun charReplacementPrioritySafeRangesAndDeletionMatchGuava() {
        val replacements = mapOf('\n' to "<newline>", '\t' to "<tab>", '&' to "<and>")
        val guava = object : GuavaArrayBasedCharEscaper(replacements, ' ', '~') {
            override fun escapeUnsafe(c: Char): CharArray = CharArray(0)
        }
        val guavaKt = object : GuavaKtArrayBasedCharEscaper(replacements, ' ', '~') {
            override fun escapeUnsafe(c: Char): CharArray = CharArray(0)
        }
        val inputs = listOf("", "plain ASCII", "\tFish &\u0000 Chips\r\n", "\uD800\uDC00\uFFFF\u007F")

        assertEquals(inputs.map(guava::escape), inputs.map(guavaKt::escape))
    }

    @Test
    fun reversedSafeRangeMatchesGuava() {
        val guava = object : GuavaArrayBasedCharEscaper(emptyMap(), 'Z', 'A') {
            override fun escapeUnsafe(c: Char): CharArray = "{$c}".toCharArray()
        }
        val guavaKt = object : GuavaKtArrayBasedCharEscaper(emptyMap(), 'Z', 'A') {
            override fun escapeUnsafe(c: Char): CharArray = "{$c}".toCharArray()
        }

        assertEquals(guava.escape("[FOO]"), guavaKt.escape("[FOO]"))
    }

    @Test
    fun supplementaryCodePointsAndMalformedInputMatchGuava() {
        val guava = object : GuavaArrayBasedUnicodeEscaper(emptyMap(), 0, 0x20000, null) {
            override fun escapeUnsafe(cp: Int): CharArray = "[$cp]".toCharArray()
        }
        val guavaKt = object : GuavaKtArrayBasedUnicodeEscaper(emptyMap(), 0, 0x20000, null) {
            override fun escapeUnsafe(cp: Int): CharArray = "[$cp]".toCharArray()
        }
        val valid = listOf("\uD800\uDC00", "\uDBFF\uDFFF", "A\uD834\uDD1EZ")

        assertEquals(valid.map(guava::escape), valid.map(guavaKt::escape))
        listOf("abc\uD800", "\uDC00abc", "\uD800x").forEach { malformed ->
            assertFailsWith<IllegalArgumentException> { guava.escape(malformed) }
            assertFailsWith<IllegalArgumentException> { guavaKt.escape(malformed) }
        }
    }

    @Test
    fun builderStateAndSnapshotsMatchGuava() {
        val guava = GuavaEscapers.builder()
            .setSafeRange('a', 'z')
            .setUnsafeReplacement("?")
            .addEscape('x', "<x>")
        val guavaKt = GuavaKtEscapers.builder()
            .setSafeRange('a', 'z')
            .setUnsafeReplacement("?")
            .addEscape('x', "<x>")
        val guavaFirst = guava.build()
        val guavaKtFirst = guavaKt.build()
        guava.addEscape('y', "<y>").setUnsafeReplacement("!")
        guavaKt.addEscape('y', "<y>").setUnsafeReplacement("!")

        val input = "Axy0"
        assertEquals(guavaFirst.escape(input), guavaKtFirst.escape(input))
        assertEquals(guava.build().escape(input), guavaKt.build().escape(input))
    }
}
