package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

@GwtCompatible
object Booleans {
    fun compare(a: Boolean, b: Boolean): Int = a.compareTo(b)

    fun contains(array: BooleanArray, target: Boolean): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: BooleanArray, target: Boolean): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun lastIndexOf(array: BooleanArray, target: Boolean): Int {
        for (i in array.indices.reversed()) if (array[i] == target) return i
        return -1
    }

    fun concat(vararg arrays: BooleanArray): BooleanArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = BooleanArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }

    fun join(separator: String, vararg array: Boolean): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val builder = StringBuilder(array.size * 7)
        builder.append(array[0])
        for (i in 1 until array.size) builder.append(separator).append(array[i])
        return builder.toString()
    }

    fun trueFirst(): Comparator<Boolean> = compareByDescending { it }
    fun falseFirst(): Comparator<Boolean> = compareBy { it }
}
