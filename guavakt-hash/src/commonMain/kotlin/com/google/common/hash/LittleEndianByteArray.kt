package dev.guavakt.hash

/** Guava LittleEndianByteArray — load 64-bit little-endian words from byte arrays. */
internal object LittleEndianByteArray {
    fun load64(input: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((input[offset + i].toLong() and 0xffL) shl (i * 8))
        }
        return result
    }

    fun load64Safely(input: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        val limit = minOf(length, 8)
        for (i in 0 until limit) {
            result = result or ((input[offset + i].toLong() and 0xffL) shl (i * 8))
        }
        return result
    }

    fun store64(sink: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            sink[offset + i] = (value shr (i * 8)).toByte()
        }
    }
}
