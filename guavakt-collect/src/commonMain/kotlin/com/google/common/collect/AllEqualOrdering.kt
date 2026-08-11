package dev.guavakt.collect

/** Guava AllEqualOrdering — comparator using natural order or delegated compare. */
open class AllEqualOrdering<T : Comparable<T>> : Comparator<T> {
    override fun compare(a: T, b: T): Int = a.compareTo(b)
    companion object {
        fun <T : Comparable<T>> instance(): AllEqualOrdering<T> = AllEqualOrdering()
    }
}
