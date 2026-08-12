package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class SipHashStreamingHasherTest {
    @Test
    fun sipHashStreamingHasherMatchesOneShotAcrossChunkBoundaries() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        val functions = listOf(
            Hashing.sipHash24(),
            Hashing.sipHash24(0x1020304050607080L, -0x102030405060708L),
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
                assertEquals(expected, hasher.hash(), "chunkSize=$chunkSize")
            }
        }
    }
}
