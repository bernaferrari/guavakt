package dev.guavakt.collect

/**
 * Guava AbstractSortedSetMultimap — [get] returns a sorted [MutableSet] of values.
 */
abstract class AbstractSortedSetMultimap<K, V> protected constructor(
    map: MutableMap<K, MutableCollection<V>>,
) : AbstractSetMultimap<K, V>(map), SortedSetMultimap<K, V> {

    protected abstract override fun createCollection(): MutableSet<V>

    abstract override fun valueComparator(): Comparator<in V>?

    override fun get(key: K): MutableSet<V> = super.get(key)
}
