package dev.guavakt.collect

/**
 * Guava SortedSetMultimap — values for each key form a sorted set.
 */
interface SortedSetMultimap<K, V> : SetMultimap<K, V> {
    fun valueComparator(): Comparator<in V>?
}
