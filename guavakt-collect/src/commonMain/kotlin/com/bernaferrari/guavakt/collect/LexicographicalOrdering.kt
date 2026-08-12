package com.bernaferrari.guavakt.collect

/** Guava LexicographicalOrdering — comparator using natural order or delegated compare. */
open class LexicographicalOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): LexicographicalOrdering<T> = LexicographicalOrdering()
    }
}
