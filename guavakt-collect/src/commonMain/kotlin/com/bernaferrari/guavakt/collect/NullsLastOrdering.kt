package com.bernaferrari.guavakt.collect

/** Guava NullsLastOrdering — comparator using natural order or delegated compare. */
open class NullsLastOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): NullsLastOrdering<T> = NullsLastOrdering()
    }
}
