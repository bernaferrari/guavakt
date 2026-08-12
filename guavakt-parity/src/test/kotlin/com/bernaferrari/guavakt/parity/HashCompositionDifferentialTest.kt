package com.bernaferrari.guavakt.parity

import com.google.common.hash.HashCode as GuavaHashCode
import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.HashCode
import com.bernaferrari.guavakt.hash.Hashing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HashCompositionDifferentialTest {
    @Test
    fun orderedAndUnorderedHashCodeCombinationMatchGuava() {
        val guavaCodes = listOf(
            GuavaHashCode.fromInt(0x01234567),
            GuavaHashCode.fromInt(0x76543210),
            GuavaHashCode.fromInt(-1),
        )
        val codes = listOf(
            HashCode.fromInt(0x01234567),
            HashCode.fromInt(0x76543210),
            HashCode.fromInt(-1),
        )
        assertEquals(GuavaHashing.combineOrdered(guavaCodes).toString(), Hashing.combineOrdered(codes).toString())
        assertEquals(GuavaHashing.combineUnordered(guavaCodes).toString(), Hashing.combineUnordered(codes).toString())
        assertEquals(
            GuavaHashing.combineOrdered(guavaCodes.reversed()).toString(),
            Hashing.combineOrdered(codes.reversed()).toString(),
        )
        assertEquals(
            GuavaHashing.combineUnordered(guavaCodes.reversed()).toString(),
            Hashing.combineUnordered(codes.reversed()).toString(),
        )
    }

    @Test
    fun concatenatingHashFunctionsMatchGuavaForOneShotAndChunkedInput() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        for (chunkSize in 1..17) {
            val guava = GuavaHashing.concatenating(
                GuavaHashing.murmur3_32_fixed(), GuavaHashing.murmur3_128(), GuavaHashing.crc32(),
            )
            val ours = Hashing.concatenating(Hashing.murmur3_32_fixed(), Hashing.murmur3_128(), Hashing.crc32())
            assertEquals(
                chunkedGuavaHash(guava, payload, chunkSize),
                chunkedGuavaKtHash(ours, payload, chunkSize),
                "chunkSize=$chunkSize",
            )
        }
    }

    @Test
    fun combinationRejectsEmptyAndMixedWidthInputLikeGuava() {
        assertFailsWith<IllegalArgumentException> { Hashing.combineOrdered(emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            Hashing.combineUnordered(listOf(HashCode.fromInt(1), HashCode.fromLong(1)))
        }
    }

    private fun chunkedGuavaHash(function: com.google.common.hash.HashFunction, bytes: ByteArray, chunkSize: Int): String {
        val hasher = function.newHasher()
        var offset = 0
        while (offset < bytes.size) {
            val length = minOf(chunkSize, bytes.size - offset)
            hasher.putBytes(bytes, offset, length)
            offset += length
        }
        return hasher.hash().toString()
    }

    private fun chunkedGuavaKtHash(function: com.bernaferrari.guavakt.hash.HashFunction, bytes: ByteArray, chunkSize: Int): String {
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
