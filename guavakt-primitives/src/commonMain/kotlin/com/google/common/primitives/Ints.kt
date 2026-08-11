package dev.guavakt.primitives

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible
object Ints {
    const val BYTES = 4
    const val MAX_POWER_OF_TWO = 1 shl 30

    fun hashCode(value: Int): Int = value

    fun compare(a: Int, b: Int): Int = a.compareTo(b)

    fun contains(array: IntArray, target: Int): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: IntArray, target: Int): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun lastIndexOf(array: IntArray, target: Int): Int {
        for (i in array.indices.reversed()) if (array[i] == target) return i
        return -1
    }

    fun min(array: IntArray): Int {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) if (array[i] < min) min = array[i]
        return min
    }

    fun max(array: IntArray): Int {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) if (array[i] > max) max = array[i]
        return max
    }

    fun concat(vararg arrays: IntArray): IntArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = IntArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }

    fun join(separator: String, vararg array: Int): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val builder = StringBuilder(array.size * 5)
        builder.append(array[0])
        for (i in 1 until array.size) builder.append(separator).append(array[i])
        return builder.toString()
    }

    fun checkedCast(value: Long): Int {
        val result = value.toInt()
        Preconditions.checkArgument(result.toLong() == value, "Out of range: %s", value)
        return result
    }

    fun saturatedCast(value: Long): Int = when {
        value > Int.MAX_VALUE -> Int.MAX_VALUE
        value < Int.MIN_VALUE -> Int.MIN_VALUE
        else -> value.toInt()
    }

    fun constrainToRange(value: Int, min: Int, max: Int): Int {
        Preconditions.checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max)
        return value.coerceIn(min, max)
    }

    fun asList(vararg backingArray: Int): List<Int> = backingArray.toList()

    fun toArray(collection: Collection<Int>): IntArray {
        val result = IntArray(collection.size)
        var i = 0
        for (value in collection) result[i++] = value
        return result
    }

    fun tryParse(string: String): Int? = tryParse(string, 10)

    fun tryParse(string: String, radix: Int): Int? {
        if (string.isEmpty()) return null
        return string.toIntOrNull(radix)
    }
}
