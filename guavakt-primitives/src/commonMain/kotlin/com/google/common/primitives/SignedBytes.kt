package dev.guavakt.primitives

import dev.guavakt.base.Preconditions

/**
 * Static utilities for signed byte primitives. Mirrors Guava SignedBytes.
 */
object SignedBytes {
    const val MAX_POWER_OF_TWO: Byte = 64

    fun checkedCast(value: Long): Byte {
        val result = value.toByte()
        Preconditions.checkArgument(result.toLong() == value, "Out of range: %s", value)
        return result
    }

    fun saturatedCast(value: Long): Byte = when {
        value > Byte.MAX_VALUE -> Byte.MAX_VALUE
        value < Byte.MIN_VALUE -> Byte.MIN_VALUE
        else -> value.toByte()
    }

    fun compare(a: Byte, b: Byte): Int = a.compareTo(b)

    fun min(vararg array: Byte): Byte {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) if (array[i] < min) min = array[i]
        return min
    }

    fun max(vararg array: Byte): Byte {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) if (array[i] > max) max = array[i]
        return max
    }

    fun join(separator: String, vararg array: Byte): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val sb = StringBuilder(array.size * 5)
        sb.append(array[0].toInt())
        for (i in 1 until array.size) sb.append(separator).append(array[i].toInt())
        return sb.toString()
    }

    fun lexicographicalComparator(): Comparator<ByteArray> = LexicographicalComparator.INSTANCE

    private enum class LexicographicalComparator : Comparator<ByteArray> {
        INSTANCE;
        override fun compare(a: ByteArray, b: ByteArray): Int {
            val min = minOf(a.size, b.size)
            for (i in 0 until min) {
                val result = SignedBytes.compare(a[i], b[i])
                if (result != 0) return result
            }
            return a.size - b.size
        }
    }

    fun sortDescending(array: ByteArray) {
        sortDescending(array, 0, array.size)
    }

    fun sortDescending(array: ByteArray, fromIndex: Int, toIndex: Int) {
        Preconditions.checkNotNull(array)
        Preconditions.checkPositionIndexes(fromIndex, toIndex, array.size)
        array.sort(fromIndex, toIndex)
        Bytes.reverse(array, fromIndex, toIndex)
    }
}
