package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Crc32cTest {
    @Test
    fun crc32c_stable_and_differs() {
        val h = Hashing.crc32c()
        val a = h.hashBytes(byteArrayOf(1, 2, 3, 4))
        val b = h.hashBytes(byteArrayOf(1, 2, 3, 4))
        assertEquals(a.asInt(), b.asInt())
        assertNotEquals(a.asInt(), h.hashBytes(byteArrayOf(1, 2, 3, 5)).asInt())
    }
}
