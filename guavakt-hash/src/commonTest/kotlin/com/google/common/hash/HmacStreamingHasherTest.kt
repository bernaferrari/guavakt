package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class HmacStreamingHasherTest {
    @Test
    fun sha2HmacHashersMatchOneShotAcrossChunkBoundariesAndLongKeys() {
        val key = ByteArray(257) { index -> (index * 11 + 7).toByte() }
        val payload = ByteArray(513) { index -> (index * 73 + 19).toByte() }
        for (function in listOf(Hashing.hmacSha256(key), Hashing.hmacSha512(key))) {
            val expected = function.hashBytes(payload)
            for (chunkSize in 1..131) {
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
}
