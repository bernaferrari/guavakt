package dev.guavakt.parity

import com.google.common.hash.HashFunction as GuavaHashFunction
import com.google.common.hash.Hashing as GuavaHashing
import dev.guavakt.hash.HashFunction as GuavaKtHashFunction
import dev.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha512StreamingDifferentialTest {
    @Test
    fun chunkedSha384AndSha512MatchGuavaAcrossDigestPaddingBoundaries() {
        for (length in listOf(0, 1, 3, 111, 112, 113, 127, 128, 129, 255, 256, 257, 513)) {
            val bytes = ByteArray(length) { index -> (index * 73 + 19).toByte() }
            for (chunkSize in 1..17) {
                assertEquivalent(GuavaHashing.sha384(), GuavaKtHashing.sha384(), bytes, chunkSize, "sha384")
                assertEquivalent(GuavaHashing.sha512(), GuavaKtHashing.sha512(), bytes, chunkSize, "sha512")
            }
        }
    }

    private fun assertEquivalent(
        guava: GuavaHashFunction,
        guavaKt: GuavaKtHashFunction,
        bytes: ByteArray,
        chunkSize: Int,
        label: String,
    ) {
        assertEquals(
            chunkedGuavaHash(guava, bytes, chunkSize),
            chunkedGuavaKtHash(guavaKt, bytes, chunkSize),
            "$label; length=${bytes.size}, chunkSize=$chunkSize",
        )
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
