package com.bernaferrari.guavakt.escape

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ArrayBasedEscaperTest {
    @Test
    fun escaperMapBuildsACompactSnapshotIncludingEmptyReplacements() {
        val input = mutableMapOf('\u0000' to "zero", 'z' to "last", 'x' to "")
        val escaperMap = ArrayBasedEscaperMap.create(input)
        input['z'] = "changed"

        val table = escaperMap.replacementArray()
        assertEquals('z'.code + 1, table.size)
        assertContentEquals("zero".toCharArray(), table[0])
        assertContentEquals(CharArray(0), table['x'.code])
        assertContentEquals("last".toCharArray(), table['z'.code])
        assertNull(table['a'.code])
        assertEquals(0, ArrayBasedEscaperMap.create(emptyMap()).replacementArray().size)
    }

    @Test
    fun charEscaperAppliesMappingsBeforeSafeRangeAndCanDeleteUnsafeChars() {
        val escaper = object : ArrayBasedCharEscaper(
            mapOf('\n' to "<newline>", '&' to "<and>"),
            ' ',
            '~',
        ) {
            override fun escapeUnsafe(c: Char): CharArray = CharArray(0)
        }

        assertEquals("Fish <and> Chips<newline>", escaper.escape("\tFish & Chips\n"))
        assertEquals("<and>", Escapers.computeReplacement(escaper, '&'))
        assertContentEquals(CharArray(0), escaper.escapeInternal('\u0000'))
    }

    @Test
    fun reversedCharSafeRangeEscapesEverything() {
        val escaper = object : ArrayBasedCharEscaper(emptyMap(), 'Z', 'A') {
            override fun escapeUnsafe(c: Char): CharArray = "{$c}".toCharArray()
        }

        assertEquals("{[}{F}{O}{O}{]}", escaper.escape("[FOO]"))
    }

    @Test
    fun unicodeEscaperTreatsSurrogatePairsAsSingleCodePoints() {
        val escaper = object : ArrayBasedUnicodeEscaper(emptyMap(), 0, 0x20000, null) {
            override fun escapeUnsafe(cp: Int): CharArray = "[$cp]".toCharArray()
        }

        val safeSupplementary = "\uD800\uDC00" // U+10000
        val maximumCodePoint = "\uDBFF\uDFFF" // U+10FFFF
        assertEquals(safeSupplementary, escaper.escape(safeSupplementary))
        assertEquals("[1114111]", escaper.escape(maximumCodePoint))
    }

    @Test
    fun baseUnicodeEscaperRejectsEveryMalformedSurrogateShape() {
        val nop = object : UnicodeEscaper() {
            override fun escape(cp: Int): CharArray? = null
        }

        assertFailsWith<IllegalArgumentException> { nop.escape("abc\uD800") }
        assertFailsWith<IllegalArgumentException> { nop.escape("\uDC00abc") }
        assertFailsWith<IllegalArgumentException> { nop.escape("abc\uDC00") }
        assertFailsWith<IllegalArgumentException> { nop.escape("\uD800x") }
    }

    @Test
    fun unicodeEscaperHandlesFalsePositiveFastPathIndices() {
        val escaper = object : UnicodeEscaper() {
            override fun escape(cp: Int): CharArray? =
                if (cp in 'a'.code..'z'.code) cp.toChar().uppercaseChar().toString().toCharArray() else null

            override fun nextEscapeIndex(csq: CharSequence, start: Int, end: Int): Int {
                var index = start
                while (index < end && !csq[index].isLetter()) index++
                return index
            }
        }

        assertEquals("\u0000HELLO \uD800\uDC00 WORLD!\n", escaper.escape("\u0000HeLLo \uD800\uDC00 WorlD!\n"))
    }

    @Test
    fun builderUsesSafeRangeUnsafeReplacementAndSnapshotsState() {
        val builder = Escapers.builder()
            .setSafeRange('a', 'z')
            .setUnsafeReplacement("?")
            .addEscape('x', "<x>")
        val first = builder.build()
        builder.addEscape('y', "<y>").setUnsafeReplacement("!")
        val second = builder.build()

        assertEquals("?<x>y?", first.escape("Axy0"))
        assertEquals("!<x><y>!", second.escape("Axy0"))
        assertSame(Escapers.nullEscaper(), Escapers.nullEscaper())
    }
}
