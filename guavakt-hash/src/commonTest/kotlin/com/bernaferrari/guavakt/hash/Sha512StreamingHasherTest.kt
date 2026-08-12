package com.bernaferrari.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha512StreamingHasherTest {
    @Test
    fun sha384AndSha512StreamingHashersMatchOneShotAcrossBlockBoundaries() {
        val payload = ByteArray(513) { index -> (index * 73 + 19).toByte() }
        for (function in listOf(Hashing.sha384(), Hashing.sha512())) {
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
