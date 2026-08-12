package com.bernaferrari.guavakt.collect

class ExplicitOrdering<T>(list: List<T>) : Ordering<T>() {
    private val rankMap: Map<T, Int> = list.withIndex().associate { it.value to it.index }
    override fun compare(left: T, right: T): Int {
        val leftRank = rankMap[left] ?: throw ClassCastException("Unknown element $left")
        val rightRank = rankMap[right] ?: throw ClassCastException("Unknown element $right")
        return leftRank.compareTo(rightRank)
    }
}
