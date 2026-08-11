package dev.guavakt.primitives

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible
object Longs {
    const val BYTES = 8
    const val MAX_POWER_OF_TWO = 1L shl 62

    fun hashCode(value: Long): Int = (value xor (value ushr 32)).toInt()

    fun compare(a: Long, b: Long): Int = a.compareTo(b)

    fun contains(array: LongArray, target: Long): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: LongArray, target: Long): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun lastIndexOf(array: LongArray, target: Long): Int {
        for (i in array.indices.reversed()) if (array[i] == target) return i
        return -1
    }

    fun min(array: LongArray): Long {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) if (array[i] < min) min = array[i]
        return min
    }

    fun max(array: LongArray): Long {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) if (array[i] > max) max = array[i]
        return max
    }

    fun concat(vararg arrays: LongArray): LongArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = LongArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }

    fun join(separator: String, vararg array: Long): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val builder = StringBuilder(array.size * 10)
        builder.append(array[0])
        for (i in 1 until array.size) builder.append(separator).append(array[i])
        return builder.toString()
    }

    fun constrainToRange(value: Long, min: Long, max: Long): Long {
        Preconditions.checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max)
        return value.coerceIn(min, max)
    }

    fun tryParse(string: String): Long? = string.toLongOrNull()
    fun tryParse(string: String, radix: Int): Long? = string.toLongOrNull(radix)

    fun reverse(array: LongArray) {
        reverse(array, 0, array.size)
    }

    fun reverse(array: LongArray, fromIndex: Int, toIndex: Int) {
        var i = fromIndex
        var j = toIndex - 1
        while (i < j) {
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
            i++
            j--
        }
    }

}
