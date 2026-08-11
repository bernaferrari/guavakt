package dev.guavakt.collect

/** Guava SortedIterable — iterable that iterates in comparator order. */
interface SortedIterable<T> : Iterable<T> {
    fun comparator(): Comparator<in T>
}
