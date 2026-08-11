package dev.guavakt.collect

/** Guava ReverseNaturalOrdering — comparator using natural order or delegated compare. */
open class ReverseNaturalOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): ReverseNaturalOrdering<T> = ReverseNaturalOrdering()
    }
}
