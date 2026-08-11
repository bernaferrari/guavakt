package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava SortedIterables — utilities for sorted collections.
 */
internal object SortedIterables {
    fun hasSameComparator(comparator: Comparator<*>, elements: Iterable<*>): Boolean {
        Preconditions.checkNotNull(comparator)
        Preconditions.checkNotNull(elements)
        val comparator2: Comparator<*> = when (elements) {
            is SortedIterable<*> -> elements.comparator()
            is SortedSet<*> -> elements.comparator() ?: Comparator { a, b -> (a as Comparable<Any>).compareTo(b as Any) }
            else -> return false
        }
        return comparator == comparator2
    }

    fun <E> comparator(sortedSet: SortedSet<E>): Comparator<in E> {
        @Suppress("UNCHECKED_CAST")
        return (sortedSet.comparator() ?: Comparator { a, b -> (a as Comparable<Any>).compareTo(b as Any) }) as Comparator<in E>
    }
}

/** Minimal SortedSet stand-in for KMP when java.util.SortedSet is unavailable as type. */
interface SortedSet<E> : Set<E> {
    fun comparator(): Comparator<in E>?
}
