package com.bernaferrari.guavakt.primitives

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions
import kotlin.math.max
import kotlin.math.min

@GwtCompatible
object Doubles {
    const val BYTES = 8

    fun hashCode(value: Double): Int = value.hashCode()
    fun compare(a: Double, b: Double): Int = a.compareTo(b)

    fun isFinite(value: Double): Boolean = value.isFinite()

    fun contains(array: DoubleArray, target: Double): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: DoubleArray, target: Double): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun min(array: DoubleArray): Double {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = array[0]
        for (i in 1 until array.size) min = min(min, array[i])
        return min
    }

    fun max(array: DoubleArray): Double {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = array[0]
        for (i in 1 until array.size) max = max(max, array[i])
        return max
    }

    fun constrainToRange(value: Double, min: Double, max: Double): Double {
        Preconditions.checkArgument(min <= max, "min (%s) must be less than or equal to max (%s)", min, max)
        return value.coerceIn(min, max)
    }

    fun join(separator: String, vararg array: Double): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val builder = StringBuilder(array.size * 12)
        builder.append(array[0])
        for (i in 1 until array.size) builder.append(separator).append(array[i])
        return builder.toString()
    }

    fun tryParse(string: String): Double? = string.toDoubleOrNull()
}
