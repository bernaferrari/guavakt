package dev.guavakt.parity

import com.google.common.hash.HashFunction as GuavaHashFunction
import com.google.common.hash.Hashing as GuavaHashing
import dev.guavakt.hash.HashFunction as GuavaKtHashFunction
import dev.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class MurmurStreamingDifferentialTest {
    @Test
    fun chunkedMurmur3HashersMatchGuavaForPositiveAndNegativeSeeds() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        for (seed in listOf(0, 17, -1, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertChunkedHashesMatch(
                "murmur3_32 seed=$seed",
                GuavaHashing.murmur3_32_fixed(seed),
                GuavaKtHashing.murmur3_32_fixed(seed),
                payload,
            )
            assertChunkedHashesMatch(
                "murmur3_128 seed=$seed",
                GuavaHashing.murmur3_128(seed),
                GuavaKtHashing.murmur3_128(seed),
                payload,
            )
        }
    }

    private fun assertChunkedHashesMatch(
        label: String,
        guava: GuavaHashFunction,
        guavaKt: GuavaKtHashFunction,
        payload: ByteArray,
    ) {
        for (chunkSize in 1..17) {
            assertEquals(
                chunkedGuavaHash(guava, payload, chunkSize),
                chunkedGuavaKtHash(guavaKt, payload, chunkSize),
                "$label; chunkSize=$chunkSize",
            )
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
