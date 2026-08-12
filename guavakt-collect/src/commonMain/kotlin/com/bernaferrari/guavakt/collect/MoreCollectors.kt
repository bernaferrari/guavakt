package com.bernaferrari.guavakt.collect

/**
 * Guava MoreCollectors — collectors without full Java Stream API on KMP.
 * Provides iterable helpers mirroring Guava's onlyElement / toOptional semantics.
 */
object MoreCollectors {
    fun <T> onlyElement(iterable: Iterable<T>): T {
        val it = iterable.iterator()
        check(it.hasNext()) { "expected one element but was: <empty>" }
        val first = it.next()
        check(!it.hasNext()) { "expected one element but was: <$first, ...>" }
        return first
    }

    fun <T : Any> toOptional(iterable: Iterable<T>): T? {
        val it = iterable.iterator()
        if (!it.hasNext()) return null
        val first = it.next()
        check(!it.hasNext()) { "expected at most one element" }
        return first
    }

    /** Accumulate iterable into a multiset (Guava often uses streams; KMP helper). */
    fun <T> toMultiset(iterable: Iterable<T>): Multiset<T> = HashMultiset.create(iterable)
}
