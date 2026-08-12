package com.bernaferrari.guavakt.parity

import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class ChecksumStreamingDifferentialTest {
    @Test
    fun crc32ChunkedHasherMatchesGuava() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        for (chunkSize in 1..17) {
            assertEquals(
                chunkedHash(GuavaHashing.crc32(), payload, chunkSize),
                chunkedHash(GuavaKtHashing.crc32(), payload, chunkSize),
                "chunkSize=$chunkSize",
            )
        }
    }

    private fun chunkedHash(function: com.google.common.hash.HashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }

    private fun chunkedHash(function: com.bernaferrari.guavakt.hash.HashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }
}
