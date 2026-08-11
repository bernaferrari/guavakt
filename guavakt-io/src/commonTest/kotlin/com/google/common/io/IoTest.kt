package dev.guavakt.io

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class IoTest {
    @Test
    fun base64_roundTrip() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val enc = BaseEncoding.base64().encode(data)
        assertContentEquals(data, BaseEncoding.base64().decode(enc))
    }

    @Test
    fun base16_encode() {
        assertEquals("000102", BaseEncoding.base16().encode(byteArrayOf(0, 1, 2)))
    }
}
