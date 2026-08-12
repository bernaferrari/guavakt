package com.bernaferrari.guavakt.io

import kotlin.test.Test
import kotlin.test.assertEquals

class StreamsExtraTest {
    @Test
    fun charStreams_copyAndLines() {
        val sb = StringBuilder()
        assertEquals(5, CharStreams.copy("hello", sb))
        assertEquals("hello", sb.toString())
        assertEquals(listOf("a", "b"), CharStreams.readLines("a\nb"))
    }

    @Test
    fun byteStreams_dataRoundTrip() {
        val out = ByteStreams.newDataOutput()
        out.write(1)
        out.write(byteArrayOf(2, 3))
        val bytes = out.toByteArray()
        val input = ByteStreams.newDataInput(bytes)
        assertEquals(1.toByte(), input.readByte())
        val buf = ByteArray(2)
        input.readFully(buf)
        assertEquals(2.toByte(), buf[0])
        assertEquals(3.toByte(), buf[1])
    }
}
