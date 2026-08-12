package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

/** Guava Lists factories — **thin aliases** to Kotlin [ArrayList] / [mutableListOf] / [listOf]. Prefer stdlib in new code. */
@GwtCompatible
object Lists {
    fun <E> newArrayList(): ArrayList<E> = ArrayList()

    fun <E> newArrayList(vararg elements: E): ArrayList<E> =
        ArrayList<E>(elements.size).apply { addAll(elements) }

    fun <E> newArrayList(elements: Iterable<E>): ArrayList<E> =
        if (elements is Collection) ArrayList(elements)
        else ArrayList<E>().apply { elements.forEach { add(it) } }

    fun <E> newArrayList(elements: Iterator<E>): ArrayList<E> =
        ArrayList<E>().apply { while (elements.hasNext()) add(elements.next()) }

    fun <E> newArrayListWithCapacity(initialArraySize: Int): ArrayList<E> {
        Preconditions.checkArgument(initialArraySize >= 0)
        return ArrayList(initialArraySize)
    }

    fun <E> newArrayListWithExpectedSize(estimatedSize: Int): ArrayList<E> {
        Preconditions.checkArgument(estimatedSize >= 0)
        return ArrayList(computeArrayListCapacity(estimatedSize))
    }

    fun <E> newLinkedList(): MutableList<E> = mutableListOf()

    fun <E> newLinkedList(elements: Iterable<E>): MutableList<E> =
        mutableListOf<E>().apply { addAll(elements) }

    fun <E> newCopyOnWriteArrayList(): MutableList<E> = ArrayList()

    fun <E> newCopyOnWriteArrayList(elements: Iterable<E>): MutableList<E> =
        ArrayList<E>().apply { addAll(elements) }

    fun <E> asList(first: E, rest: Array<out E>): List<E> =
        listOf(first) + rest.toList()

    fun <E> asList(first: E, second: E, rest: Array<out E>): List<E> =
        listOf(first, second) + rest.toList()

    fun <B> cartesianProduct(lists: List<List<B>>): List<List<B>> =
        CartesianList.create(lists)

    fun <B> cartesianProduct(vararg lists: List<B>): List<List<B>> =
        cartesianProduct(lists.toList())

    fun <F, T> transform(fromList: List<F>, function: (F) -> T): List<T> =
        object : AbstractList<T>() {
            override val size: Int get() = fromList.size
            override fun get(index: Int): T = function(fromList[index])
        }

    fun <E> partition(list: List<E>, size: Int): List<List<E>> {
        Preconditions.checkNotNull(list)
        Preconditions.checkArgument(size > 0)
        return list.chunked(size)
    }

    fun <E> reverse(list: List<E>): List<E> =
        object : AbstractList<E>() {
            override val size: Int get() = list.size
            override fun get(index: Int): E = list[list.size - 1 - index]
        }

    fun charactersOf(sequence: CharSequence): List<Char> =
        object : AbstractList<Char>() {
            override val size: Int get() = sequence.length
            override fun get(index: Int): Char = sequence[index]
        }

    fun <E : Comparable<E>> sort(list: MutableList<E>) {
        list.sort()
    }

    fun <E> sort(list: MutableList<E>, comparator: Comparator<in E>) {
        list.sortWith(comparator)
    }

    fun <E> sortedCopy(list: Iterable<E>): List<E> where E : Comparable<E> =
        list.sorted()

    fun <E> sortedCopy(list: Iterable<E>, comparator: Comparator<in E>): List<E> =
        list.sortedWith(comparator)

    fun <T> newArrayListWithCapacity(initialArraySize: Int, unused: Boolean): ArrayList<T> =
        newArrayListWithCapacity(initialArraySize)

    /** Guava computeArrayListCapacity */
    internal fun computeArrayListCapacity(arraySize: Int): Int {
        CollectPreconditions.checkNonnegative(arraySize, "arraySize")
        return minOf(Int.MAX_VALUE.toLong(), 5L + arraySize + (arraySize / 10)).toInt()
    }
}
