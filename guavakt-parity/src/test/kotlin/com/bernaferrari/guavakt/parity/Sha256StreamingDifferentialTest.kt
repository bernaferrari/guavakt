package com.bernaferrari.guavakt.parity

import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256StreamingDifferentialTest {
    @Test
    fun chunkedSha256MatchesGuavaAcrossDigestPaddingBoundaries() {
        for (length in listOf(0, 1, 3, 55, 56, 57, 63, 64, 65, 127, 128, 129, 257)) {
            val bytes = ByteArray(length) { index -> (index * 73 + 19).toByte() }
            for (chunkSize in 1..17) {
                assertEquals(
                    chunkedGuavaHash(bytes, chunkSize),
                    chunkedGuavaKtHash(bytes, chunkSize),
                    "length=$length, chunkSize=$chunkSize",
                )
            }
        }
    }

    private fun chunkedGuavaHash(bytes: ByteArray, chunkSize: Int): String {
        val hasher = GuavaHashing.sha256().newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }

    private fun chunkedGuavaKtHash(bytes: ByteArray, chunkSize: Int): String {
        val hasher = GuavaKtHashing.sha256().newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }
}
