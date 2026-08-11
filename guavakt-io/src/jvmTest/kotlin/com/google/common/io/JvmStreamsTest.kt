package dev.guavakt.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmStreamsTest {
    @Test
    fun charStreams_reader() {
        assertEquals("hi", CharStreams.toString(StringReader("hi")))
        val sb = StringBuilder()
        assertEquals(2, CharStreams.copy(StringReader("hi"), sb))
        assertEquals("hi", sb.toString())
    }

    @Test
    fun byteStreams_inputStream() {
        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(bytes.toList(), ByteStreams.toByteArray(ByteArrayInputStream(bytes)).toList())
        val out = ByteArrayOutputStream()
        assertEquals(3, ByteStreams.copy(ByteArrayInputStream(bytes), out))
        assertEquals(bytes.toList(), out.toByteArray().toList())
    }
}
