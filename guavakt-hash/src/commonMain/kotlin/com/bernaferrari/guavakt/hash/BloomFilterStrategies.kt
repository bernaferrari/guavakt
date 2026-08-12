package com.bernaferrari.guavakt.hash

/**
 * Guava BloomFilterStrategies — MURMUR128_MITZ_32 / MURMUR128_MITZ_64 bit-setting strategies.
 */
internal enum class BloomFilterStrategies {
    MURMUR128_MITZ_32,
    MURMUR128_MITZ_64;

    fun put(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        return when (this) {
            MURMUR128_MITZ_32 -> put32(objectHash, numHashFunctions, bits)
            MURMUR128_MITZ_64 -> put64(objectHash, numHashFunctions, bits)
        }
    }

    fun mightContain(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        return when (this) {
            MURMUR128_MITZ_32 -> might32(objectHash, numHashFunctions, bits)
            MURMUR128_MITZ_64 -> might64(objectHash, numHashFunctions, bits)
        }
    }

    private fun put32(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        val bitSize = bits.bitSize()
        val hash1 = objectHash.toInt()
        val hash2 = (objectHash ushr 32).toInt()
        var bitsChanged = false
        for (i in 1..numHashFunctions) {
            var combined = hash1 + i * hash2
            if (combined < 0) combined = combined.inv()
            bitsChanged = bits.set(combined % bitSize) || bitsChanged
        }
        return bitsChanged
    }

    private fun might32(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        val bitSize = bits.bitSize()
        val hash1 = objectHash.toInt()
        val hash2 = (objectHash ushr 32).toInt()
        for (i in 1..numHashFunctions) {
            var combined = hash1 + i * hash2
            if (combined < 0) combined = combined.inv()
            if (!bits.get(combined % bitSize)) return false
        }
        return true
    }

    private fun put64(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        val bitSize = bits.bitSize()
        val hash1 = objectHash
        val hash2 = (objectHash ushr 32) or (objectHash shl 32)
        var bitsChanged = false
        var combined = hash1
        for (i in 0 until numHashFunctions) {
            bitsChanged = bits.set((combined and Long.MAX_VALUE) % bitSize) || bitsChanged
            combined += hash2
        }
        return bitsChanged
    }

    private fun might64(objectHash: Long, numHashFunctions: Int, bits: BitArray): Boolean {
        val bitSize = bits.bitSize()
        val hash1 = objectHash
        val hash2 = (objectHash ushr 32) or (objectHash shl 32)
        var combined = hash1
        for (i in 0 until numHashFunctions) {
            if (!bits.get((combined and Long.MAX_VALUE) % bitSize)) return false
            combined += hash2
        }
        return true
    }

    interface BitArray {
        fun bitSize(): Long
        fun set(bitIndex: Long): Boolean
        fun get(bitIndex: Long): Boolean
    }
}
