package dev.guavakt.collect

/** Guava NullsFirstOrdering — comparator using natural order or delegated compare. */
open class NullsFirstOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): NullsFirstOrdering<T> = NullsFirstOrdering()
    }
}
