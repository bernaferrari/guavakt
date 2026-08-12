package com.bernaferrari.guavakt.collect

/**
 * Guava Serialization helpers — KMP has no Java ObjectOutputStream; provides count/populate patterns
 * used by multimap/multiset implementations for logical serialization shape.
 */
internal object Serialization {
    fun <E> populateMultiset(multiset: Multiset<E>, distinct: Int, getCount: (Int) -> Pair<E, Int>) {
        for (i in 0 until distinct) {
            val (e, count) = getCount(i)
            multiset.add(e, count)
        }
    }

    fun <K, V> populateMultimap(multimap: Multimap<K, V>, distinctKeys: Int, getEntry: (Int) -> Pair<K, Collection<V>>) {
        for (i in 0 until distinctKeys) {
            val (k, values) = getEntry(i)
            multimap.putAll(k, values)
        }
    }
}
