package com.bernaferrari.guavakt.collect

/** Guava CompoundOrdering — comparator using natural order or delegated compare. */
open class CompoundOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): CompoundOrdering<T> = CompoundOrdering()
    }
}
