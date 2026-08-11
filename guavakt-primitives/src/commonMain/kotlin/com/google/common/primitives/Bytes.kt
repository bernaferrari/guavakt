package dev.guavakt.primitives

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible
object Bytes {
    const val BYTES = 1

    fun hashCode(value: Byte): Int = value.toInt()

    fun contains(array: ByteArray, target: Byte): Boolean {
        for (value in array) if (value == target) return true
        return false
    }

    fun indexOf(array: ByteArray, target: Byte): Int {
        for (i in array.indices) if (array[i] == target) return i
        return -1
    }

    fun lastIndexOf(array: ByteArray, target: Byte): Int {
        for (i in array.indices.reversed()) if (array[i] == target) return i
        return -1
    }

    fun concat(vararg arrays: ByteArray): ByteArray {
        var length = 0
        for (array in arrays) length += array.size
        val result = ByteArray(length)
        var pos = 0
        for (array in arrays) {
            array.copyInto(result, pos)
            pos += array.size
        }
        return result
    }

    fun asList(vararg backingArray: Byte): List<Byte> = backingArray.toList()

    fun reverse(array: ByteArray) {
        reverse(array, 0, array.size)
    }

    fun reverse(array: ByteArray, fromIndex: Int, toIndex: Int) {
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
