package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class MurmurStreamingHasherTest {
    private val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }

    @Test
    fun streamingHashersMatchOneShotHashesAtEveryRelevantChunkBoundary() {
        val functions = listOf(
            Hashing.murmur3_32(0),
            Hashing.murmur3_32(-1),
            Hashing.murmur3_32(Int.MIN_VALUE),
            Hashing.murmur3_128(0),
            Hashing.murmur3_128(-1),
            Hashing.murmur3_128(Int.MIN_VALUE),
        )

        for (function in functions) {
            val expected = function.hashBytes(payload)
            for (chunkSize in 1..17) {
                val hasher = function.newHasher()
                var offset = 0
                while (offset < payload.size) {
                    val length = minOf(chunkSize, payload.size - offset)
                    hasher.putBytes(payload, offset, length)
                    offset += length
                }
                assertEquals(expected, hasher.hash(), "${function.bits()} bits; chunkSize=$chunkSize")
            }
        }
    }

    @Test
    fun streamingHashersHandleAnEmptyInput() {
        for (function in listOf(Hashing.murmur3_32(), Hashing.murmur3_128())) {
            assertEquals(function.hashBytes(byteArrayOf()), function.newHasher().hash())
        }
    }
}
