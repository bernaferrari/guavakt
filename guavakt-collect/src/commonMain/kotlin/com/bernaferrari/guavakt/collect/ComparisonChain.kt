package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
class ComparisonChain private constructor(private val result: Int) {
    fun compare(left: Comparable<*>?, right: Comparable<*>?): ComparisonChain {
        if (result != 0) return this
        @Suppress("UNCHECKED_CAST")
        val l = left as Comparable<Any?>?
        @Suppress("UNCHECKED_CAST")
        val r = right as Comparable<Any?>?
        val cmp = when {
            l === r -> 0
            l == null -> -1
            r == null -> 1
            else -> l.compareTo(r)
        }
        return classify(cmp)
    }
    fun compare(left: Int, right: Int): ComparisonChain =
        if (result != 0) this else classify(left.compareTo(right))
    fun compare(left: Long, right: Long): ComparisonChain =
        if (result != 0) this else classify(left.compareTo(right))
    fun compare(left: Boolean, right: Boolean): ComparisonChain =
        if (result != 0) this else classify(left.compareTo(right))
    fun <T> compare(left: T, right: T, comparator: Comparator<T>): ComparisonChain =
        if (result != 0) this else classify(comparator.compare(left, right))
    fun compareTrueFirst(left: Boolean, right: Boolean): ComparisonChain = compare(right, left)
    fun compareFalseFirst(left: Boolean, right: Boolean): ComparisonChain = compare(left, right)
    fun result(): Int = result
    companion object {
        private val ACTIVE = ComparisonChain(0)
        private val LESS = ComparisonChain(-1)
        private val GREATER = ComparisonChain(1)
        fun start(): ComparisonChain = ACTIVE
        private fun classify(result: Int): ComparisonChain = when {
            result < 0 -> LESS
            result > 0 -> GREATER
            else -> ACTIVE
        }
    }
}
