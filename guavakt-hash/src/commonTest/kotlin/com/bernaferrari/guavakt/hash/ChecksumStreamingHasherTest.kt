package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class ChecksumStreamingHasherTest {
    @Test
    fun crc32StreamingHasherMatchesOneShotAcrossChunkBoundaries() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        val function = Hashing.crc32()
        val expected = function.hashBytes(payload)

        for (chunkSize in 1..17) {
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
}
