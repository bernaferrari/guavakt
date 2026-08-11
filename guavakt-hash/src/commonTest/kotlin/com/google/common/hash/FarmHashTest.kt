package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FarmHashTest {
    @Test
    fun farmHash_stable_differsByInput() {
        val h = Hashing.farmHashFingerprint64()
        val a = h.hashUnencodedChars("hello")
        val b = h.hashUnencodedChars("hello")
        assertEquals(a.asLong(), b.asLong())
        assertNotEquals(a.asLong(), h.hashUnencodedChars("world").asLong())
    }
}
