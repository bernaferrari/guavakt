package com.bernaferrari.guavakt.collect

/** Guava ByFunctionOrdering — comparator using natural order or delegated compare. */
open class ByFunctionOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): ByFunctionOrdering<T> = ByFunctionOrdering()
    }
}
