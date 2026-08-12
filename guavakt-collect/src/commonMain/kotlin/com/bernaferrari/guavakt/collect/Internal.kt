package com.bernaferrari.guavakt.collect

/** Guava collect Internal — shared helpers. */
internal object Internal {
    fun <T> toArray(collection: Collection<T>): Array<Any?> {
        val result = arrayOfNulls<Any?>(collection.size)
        var i = 0
        for (e in collection) result[i++] = e
        return result
    }

    fun <E> checkElement(index: Int, size: Int) {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("index=$index size=$size")
    }
}
