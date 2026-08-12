package com.bernaferrari.guavakt.collect

internal class ComparatorOrdering<T>(private val comparator: Comparator<T>) : Ordering<T>() {
    override fun compare(left: T, right: T): Int = comparator.compare(left, right)
}
