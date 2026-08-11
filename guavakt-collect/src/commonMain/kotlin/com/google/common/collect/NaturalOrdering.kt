package dev.guavakt.collect

/** Guava NaturalOrdering — compare Comparables. */
internal object NaturalOrdering : Ordering<Comparable<Any>>() {
    override fun compare(left: Comparable<Any>, right: Comparable<Any>): Int = left.compareTo(right)
}
