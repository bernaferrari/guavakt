package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Sha256StreamingHasherTest {
    @Test
    fun sha256StreamingHasherMatchesOneShotAcrossBlockAndChunkBoundaries() {
        val payload = ByteArray(257) { index -> (index * 73 + 19).toByte() }
        val function = Hashing.sha256()
        val expected = function.hashBytes(payload)
        for (chunkSize in 1..67) {
            val hasher = function.newHasher()
            var offset = 0
            while (offset < payload.size) {
                val length = minOf(chunkSize, payload.size - offset)
                hasher.putBytes(payload, offset, length)
                offset += length
            }
            assertEquals(expected, hasher.hash(), "chunkSize=$chunkSize")
        }
    }

    @Test
    fun sha256HasherDoesNotAcceptWritesAfterFinalization() {
        val hasher = Hashing.sha256().newHasher()
        hasher.hash()
        assertFailsWith<IllegalStateException> { hasher.putByte(1) }
    }
}
