package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Guava ObjectArrays — static utilities for object arrays (KMP uses Array<Any?>).
 */
object ObjectArrays {
    @Suppress("UNCHECKED_CAST")
    fun <T> newArray(length: Int): Array<T?> = arrayOfNulls(length)

    @Suppress("UNCHECKED_CAST")
    fun <T> newArray(reference: Array<T?>, length: Int): Array<T?> {
        val result = arrayOfNulls<Any?>(length) as Array<T?>
        return result
    }

    fun <T> concat(first: Array<T?>, second: Array<T?>): Array<T?> {
        @Suppress("UNCHECKED_CAST")
        val result = arrayOfNulls<Any?>(first.size + second.size) as Array<T?>
        first.copyInto(result, 0)
        second.copyInto(result, first.size)
        return result
    }

    fun <T> concat(element: T?, array: Array<T?>): Array<T?> {
        @Suppress("UNCHECKED_CAST")
        val result = arrayOfNulls<Any?>(array.size + 1) as Array<T?>
        result[0] = element
        array.copyInto(result, 1)
        return result
    }

    fun <T> concat(array: Array<T?>, element: T?): Array<T?> {
        @Suppress("UNCHECKED_CAST")
        val result = arrayOfNulls<Any?>(array.size + 1) as Array<T?>
        array.copyInto(result, 0)
        result[array.size] = element
        return result
    }

    fun <T> checkElementsNotNull(array: Array<T?>): Array<T?> {
        for (i in array.indices) checkElementNotNull(array[i], i)
        return array
    }

    fun <T> checkElementsNotNull(array: Array<T?>, offset: Int, length: Int): Array<T?> {
        Preconditions.checkPositionIndexes(offset, offset + length, array.size)
        for (i in offset until offset + length) checkElementNotNull(array[i], i)
        return array
    }

    private fun checkElementNotNull(element: Any?, index: Int): Any? {
        if (element == null) throw NullPointerException("at index $index")
        return element
    }

    fun <T> toArrayImpl(c: Collection<T>, array: Array<T?>): Array<T?> {
        val size = c.size
        @Suppress("UNCHECKED_CAST")
        val result = if (array.size < size) arrayOfNulls<Any?>(size) as Array<T?> else array
        var i = 0
        for (e in c) result[i++] = e
        if (result.size > size) result[size] = null
        return result
    }

    fun <T> toArrayImpl(c: Collection<T>): Array<Any?> {
        val arr = arrayOfNulls<Any?>(c.size)
        var i = 0
        for (e in c) arr[i++] = e
        return arr
    }

    fun <T> copyAsObjectArray(elements: Array<T?>, offset: Int, length: Int): Array<Any?> {
        Preconditions.checkPositionIndexes(offset, offset + length, elements.size)
        return Array(length) { elements[offset + it] }
    }

    fun swap(array: Array<Any?>, i: Int, j: Int) {
        val temp = array[i]
        array[i] = array[j]
        array[j] = temp
    }
}
