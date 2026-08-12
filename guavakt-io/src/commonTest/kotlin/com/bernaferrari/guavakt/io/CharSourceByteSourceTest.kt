package com.bernaferrari.guavakt.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharSourceByteSourceTest {
    @Test
    fun utf8ByteSourceStreamsSupplementaryCharactersAcrossTinyReads() {
        val input = "A\uD83D\uDE00é"
        val stream = ReaderInputStream(CharReaderLike(input))
        val actual = ArrayList<Byte>()
        val oneByte = ByteArray(1)
        while (true) {
            val read = stream.read(oneByte)
            if (read < 0) break
            actual += oneByte[0]
        }
        assertContentEquals(input.encodeToByteArray(), actual.toByteArray())
        assertContentEquals(input.encodeToByteArray(), CharSource.wrap(input).asByteSource().read())
        val malformed = "unpaired-high-\uD83D"
        assertContentEquals(malformed.encodeToByteArray(), CharSource.wrap(malformed).asByteSource().read())
    }

    @Test
    fun byteSourceRejectsUnsupportedCommonCharset() {
        assertFailsWith<IllegalArgumentException> {
            CharSource.wrap("text").asByteSource("ISO-8859-1")
        }
    }

    @Test
    fun byteSourceClosesTheReaderAfterConsumption() {
        var closes = 0
        val source = CharSource.fromReader {
            object : CharReaderLike("text") {
                override fun close() {
                    closes++
                }
            }
        }

        assertEquals("text", source.asByteSource().read().decodeToString())
        assertEquals(1, closes)
    }
}
