package com.bernaferrari.guavakt.collect

/** Guava UsingToStringOrdering — comparator using natural order or delegated compare. */
open class UsingToStringOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): UsingToStringOrdering<T> = UsingToStringOrdering()
    }
}
