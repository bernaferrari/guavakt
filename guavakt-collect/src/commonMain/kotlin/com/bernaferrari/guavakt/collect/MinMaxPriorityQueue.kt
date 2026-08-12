package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Guava MinMaxPriorityQueue — double-ended priority queue (min and max in O(1) peek).
 * Implemented with two heaps / sorted multiset simulation via ArrayList + sort for clarity on KMP.
 */
class MinMaxPriorityQueue<E> private constructor(
    private val comparator: Comparator<in E>,
    private val maximumSize: Int,
    private val elements: ArrayList<E> = ArrayList(),
) : AbstractMutableCollection<E>() {

    override val size: Int get() = elements.size

    override fun iterator(): MutableIterator<E> = elements.toMutableList().iterator()

    override fun add(element: E): Boolean {
        offer(element)
        return true
    }

    fun offer(element: E): Boolean {
        elements.add(element)
        elements.sortWith(comparator)
        if (elements.size > maximumSize) {
            elements.removeAt(elements.size - 1) // drop max when over capacity
        }
        return true
    }

    fun peekFirst(): E? = elements.firstOrNull()
    fun peekLast(): E? = elements.lastOrNull()
    fun peek(): E? = peekFirst()

    fun pollFirst(): E? = if (elements.isEmpty()) null else elements.removeAt(0)
    fun pollLast(): E? = if (elements.isEmpty()) null else elements.removeAt(elements.size - 1)
    fun poll(): E? = pollFirst()

    fun removeFirst(): E = pollFirst() ?: throw NoSuchElementException()
    fun removeLast(): E = pollLast() ?: throw NoSuchElementException()

    override fun clear() { elements.clear() }

    companion object {
        fun <E : Comparable<E>> create(): MinMaxPriorityQueue<E> =
            orderedBy(naturalOrder<E>()).create()

        fun <E : Comparable<E>> create(initialContents: Iterable<E>): MinMaxPriorityQueue<E> =
            orderedBy(naturalOrder<E>()).create(initialContents)

        fun <B> orderedBy(comparator: Comparator<B>): Builder<B> = Builder(comparator)

        fun expectedSize(expectedSize: Int): Builder<Comparable<Any>> {
            @Suppress("UNCHECKED_CAST")
            return Builder(naturalOrder<Comparable<Any>>() as Comparator<Comparable<Any>>)
                .expectedSize(expectedSize)
        }

        fun maximumSize(maximumSize: Int): Builder<Comparable<Any>> {
            @Suppress("UNCHECKED_CAST")
            return Builder(naturalOrder<Comparable<Any>>() as Comparator<Comparable<Any>>)
                .maximumSize(maximumSize)
        }
    }

    class Builder<B>(private val comparator: Comparator<B>) {
        private var expectedSize = 11
        private var maximumSize = Int.MAX_VALUE

        fun expectedSize(expectedSize: Int): Builder<B> = apply {
            Preconditions.checkArgument(expectedSize >= 0)
            this.expectedSize = expectedSize
        }

        fun maximumSize(maximumSize: Int): Builder<B> = apply {
            Preconditions.checkArgument(maximumSize > 0)
            this.maximumSize = maximumSize
        }

        fun <T : B> create(): MinMaxPriorityQueue<T> {
            @Suppress("UNCHECKED_CAST")
            return MinMaxPriorityQueue(comparator as Comparator<in T>, maximumSize)
        }

        fun <T : B> create(initialContents: Iterable<T>): MinMaxPriorityQueue<T> {
            val q = create<T>()
            for (e in initialContents) q.offer(e)
            return q
        }
    }
}
