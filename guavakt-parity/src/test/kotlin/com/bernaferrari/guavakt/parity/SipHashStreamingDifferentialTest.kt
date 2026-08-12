package com.bernaferrari.guavakt.parity

import com.google.common.hash.HashFunction as GuavaHashFunction
import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.HashFunction as GuavaKtHashFunction
import com.bernaferrari.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class SipHashStreamingDifferentialTest {
    @Test
    fun sipHash24ChunkedHasherMatchesGuavaForDefaultAndCustomKeys() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        val keys = listOf(
            0x0706050403020100L to 0x0f0e0d0c0b0a0908L,
            0x1020304050607080L to -0x102030405060708L,
        )
        for ((k0, k1) in keys) {
            for (chunkSize in 1..17) {
                assertEquals(
                    chunkedGuavaHash(GuavaHashing.sipHash24(k0, k1), payload, chunkSize),
                    chunkedGuavaKtHash(GuavaKtHashing.sipHash24(k0, k1), payload, chunkSize),
                    "k0=$k0, k1=$k1, chunkSize=$chunkSize",
                )
            }
        }
    }

    private fun chunkedGuavaHash(function: GuavaHashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }

    private fun chunkedGuavaKtHash(function: GuavaKtHashFunction, bytes: ByteArray, chunkSize: Int): String {
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
