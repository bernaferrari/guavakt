package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/** Guava TopKSelector — keeps the k least (or greatest) elements seen. */
class TopKSelector<T> private constructor(
    private val k: Int,
    private val comparator: Comparator<in T>,
    private val buffer: ArrayList<T> = ArrayList(k + 1),
) {
    fun offer(element: T) {
        if (k == 0) return
        if (buffer.size < k) {
            buffer.add(element)
            if (buffer.size == k) buffer.sortWith(comparator)
        } else if (comparator.compare(element, buffer[k - 1]) < 0) {
            buffer[k - 1] = element
            buffer.sortWith(comparator)
        }
    }

    fun offerAll(elements: Iterable<T>) {
        for (e in elements) offer(e)
    }

    fun topK(): List<T> = buffer.sortedWith(comparator)

    companion object {
        fun <T : Comparable<T>> least(k: Int): TopKSelector<T> = least(k, naturalOrder())
        fun <T> least(k: Int, comparator: Comparator<in T>): TopKSelector<T> {
            Preconditions.checkArgument(k >= 0)
            return TopKSelector(k, comparator)
        }
        fun <T : Comparable<T>> greatest(k: Int): TopKSelector<T> = greatest(k, naturalOrder())
        fun <T> greatest(k: Int, comparator: Comparator<in T>): TopKSelector<T> {
            Preconditions.checkArgument(k >= 0)
            return TopKSelector(k, comparator.reversed())
        }
    }
}
