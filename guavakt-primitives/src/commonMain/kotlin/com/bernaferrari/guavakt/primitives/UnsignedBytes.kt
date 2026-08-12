package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Static utilities for unsigned byte values (0..255). Mirrors Guava UnsignedBytes.
 */
object UnsignedBytes {
    const val MAX_POWER_OF_TWO: Byte = -0x80 // 0x80 as signed byte
    const val MAX_VALUE: Byte = -1 // 0xFF as signed byte
    private const val UNSIGNED_MASK = 0xFF

    fun toInt(value: Byte): Int = value.toInt() and UNSIGNED_MASK

    fun checkedCast(value: Long): Byte {
        Preconditions.checkArgument(value shr 8 == 0L, "out of range: %s", value)
        return value.toByte()
    }

    fun saturatedCast(value: Long): Byte = when {
        value > 255L -> MAX_VALUE
        value < 0L -> 0
        else -> value.toByte()
    }

    fun compare(a: Byte, b: Byte): Int = toInt(a) - toInt(b)

    fun min(vararg array: Byte): Byte {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = toInt(array[0])
        for (i in 1 until array.size) {
            val next = toInt(array[i])
            if (next < min) min = next
        }
        return min.toByte()
    }

    fun max(vararg array: Byte): Byte {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = toInt(array[0])
        for (i in 1 until array.size) {
            val next = toInt(array[i])
            if (next > max) max = next
        }
        return max.toByte()
    }

    fun join(separator: String, vararg array: Byte): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val sb = StringBuilder(array.size * 5)
        sb.append(toInt(array[0]))
        for (i in 1 until array.size) sb.append(separator).append(toInt(array[i]))
        return sb.toString()
    }

    fun lexicographicalComparator(): Comparator<ByteArray> = LexicographicalComparator.INSTANCE

    private enum class LexicographicalComparator : Comparator<ByteArray> {
        INSTANCE;
        override fun compare(a: ByteArray, b: ByteArray): Int {
            val min = minOf(a.size, b.size)
            for (i in 0 until min) {
                val result = UnsignedBytes.compare(a[i], b[i])
                if (result != 0) return result
            }
            return a.size - b.size
        }
    }

    fun sort(array: ByteArray) {
        sort(array, 0, array.size)
    }

    fun sort(array: ByteArray, fromIndex: Int, toIndex: Int) {
        Preconditions.checkNotNull(array)
        Preconditions.checkPositionIndexes(fromIndex, toIndex, array.size)
        // Sort by unsigned order via flip
        for (i in fromIndex until toIndex) {
            array[i] = (array[i].toInt() xor 0x80).toByte()
        }
        array.sort(fromIndex, toIndex)
        for (i in fromIndex until toIndex) {
            array[i] = (array[i].toInt() xor 0x80).toByte()
        }
    }

    fun sortDescending(array: ByteArray) {
        sortDescending(array, 0, array.size)
    }

    fun sortDescending(array: ByteArray, fromIndex: Int, toIndex: Int) {
        sort(array, fromIndex, toIndex)
        Bytes.reverse(array, fromIndex, toIndex)
    }

    fun parseUnsignedByte(string: String): Byte = parseUnsignedByte(string, 10)

    fun parseUnsignedByte(string: String, radix: Int): Byte {
        val parsed = string.toInt(radix)
        Preconditions.checkArgument(parsed shr 8 == 0, "out of range: %s", string)
        return parsed.toByte()
    }

    fun toString(x: Byte): String = toString(x, 10)

    fun toString(x: Byte, radix: Int): String = toInt(x).toString(radix)
}
