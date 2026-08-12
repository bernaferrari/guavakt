package com.bernaferrari.guavakt.parity

import com.google.common.hash.HashFunction as GuavaHashFunction
import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.HashFunction as GuavaKtHashFunction
import com.bernaferrari.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HmacStreamingDifferentialTest {
    @Test
    fun chunkedSha2HmacMatchesGuavaForShortAndLongKeys() {
        val payload = ByteArray(257) { index -> (index * 73 + 19).toByte() }
        for (keySize in listOf(1, 63, 64, 65, 127, 128, 129, 257)) {
            val key = ByteArray(keySize) { index -> (index * 11 + 7).toByte() }
            assertEquivalent(GuavaHashing.hmacSha256(key), GuavaKtHashing.hmacSha256(key), payload, "hmacSha256; keySize=$keySize")
            assertEquivalent(GuavaHashing.hmacSha512(key), GuavaKtHashing.hmacSha512(key), payload, "hmacSha512; keySize=$keySize")
        }
    }

    @Test
    fun hmacFactoriesRejectEmptyKeysLikeGuava() {
        assertFailsWith<IllegalArgumentException> { GuavaHashing.hmacSha256(byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { GuavaKtHashing.hmacSha256(byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { GuavaHashing.hmacSha512(byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { GuavaKtHashing.hmacSha512(byteArrayOf()) }
    }

    private fun assertEquivalent(guava: GuavaHashFunction, guavaKt: GuavaKtHashFunction, bytes: ByteArray, label: String) {
        for (chunkSize in 1..17) {
            assertEquals(
                chunkedGuavaHash(guava, bytes, chunkSize),
                chunkedGuavaKtHash(guavaKt, bytes, chunkSize),
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
