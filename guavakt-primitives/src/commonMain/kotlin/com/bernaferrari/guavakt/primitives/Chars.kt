package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

@GwtCompatible
object Chars {
    const val BYTES = 2

    fun hashCode(value: Char): Int = value.code
    fun compare(a: Char, b: Char): Int = a.compareTo(b)

    fun checkedCast(value: Long): Char {
        val result = value.toInt().toChar()
        Preconditions.checkArgument(result.code.toLong() == value, "Out of range: %s", value)
        return result
    }

    fun saturatedCast(value: Long): Char = when {
        value > Char.MAX_VALUE.code -> Char.MAX_VALUE
        value < Char.MIN_VALUE.code -> Char.MIN_VALUE
        else -> value.toInt().toChar()
    }

    fun contains(array: CharArray, target: Char): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: CharArray, target: Char): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun min(array: CharArray): Char {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) if (array[i] < min) min = array[i]
        return min
    }

    fun max(array: CharArray): Char {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) if (array[i] > max) max = array[i]
        return max
    }

    fun concat(vararg arrays: CharArray): CharArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = CharArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }
}
