package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

@GwtCompatible
object Shorts {
    const val BYTES = 2
    const val MAX_POWER_OF_TWO: Short = 16384

    fun hashCode(value: Short): Int = value.toInt()
    fun compare(a: Short, b: Short): Int = a.compareTo(b)

    fun checkedCast(value: Long): Short {
        val result = value.toShort()
        Preconditions.checkArgument(result.toLong() == value, "Out of range: %s", value)
        return result
    }

    fun saturatedCast(value: Long): Short = when {
        value > Short.MAX_VALUE -> Short.MAX_VALUE
        value < Short.MIN_VALUE -> Short.MIN_VALUE
        else -> value.toShort()
    }

    fun contains(array: ShortArray, target: Short): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: ShortArray, target: Short): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun min(array: ShortArray): Short {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) if (array[i] < min) min = array[i]
        return min
    }

    fun max(array: ShortArray): Short {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) if (array[i] > max) max = array[i]
        return max
    }

    fun concat(vararg arrays: ShortArray): ShortArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = ShortArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }
}
