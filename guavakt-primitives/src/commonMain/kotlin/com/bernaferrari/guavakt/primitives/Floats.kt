package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions
import kotlin.math.max
import kotlin.math.min

@GwtCompatible
object Floats {
    const val BYTES = 4

    fun hashCode(value: Float): Int = value.hashCode()
    fun compare(a: Float, b: Float): Int = a.compareTo(b)
    fun isFinite(value: Float): Boolean = value.isFinite()

    fun contains(array: FloatArray, target: Float): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: FloatArray, target: Float): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun min(array: FloatArray): Float {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) min = min(min, array[i])
        return min
    }

    fun max(array: FloatArray): Float {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) max = max(max, array[i])
        return max
    }

    fun tryParse(string: String): Float? = string.toFloatOrNull()
}
