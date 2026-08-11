package dev.guavakt.hash

import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeHashFunctionTest {
    @Test
    fun concatenatingHasherMatchesOneShotForEveryChunkBoundary() {
        val payload = ByteArray(97) { index -> (index * 73 + 19).toByte() }
        val function = Hashing.concatenating(Hashing.murmur3_32(), Hashing.murmur3_128(), Hashing.crc32())
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

    @Test
    fun orderedAndUnorderedCombinationHaveTheirExpectedOrderSemantics() {
        val first = HashCode.fromInt(0x01234567)
        val second = HashCode.fromInt(0x76543210)
        assertEquals(Hashing.combineOrdered(listOf(first, second)), Hashing.combineOrdered(listOf(first, second)))
        assertEquals(Hashing.combineUnordered(listOf(first, second)), Hashing.combineUnordered(listOf(second, first)))
    }
}
