package com.bernaferrari.guavakt.collect

/**
 * Guava RangeMap — map from disjoint ranges to values.
 */
interface RangeMap<K : Comparable<K>, V> {
    operator fun get(key: K): V?
    fun getEntry(key: K): Map.Entry<Range<K>, V>?
    fun span(): Range<K>
    fun put(range: Range<K>, value: V)
    /** Put and merge with adjacent/connected ranges that hold the same [value]. */
    fun putCoalescing(range: Range<K>, value: V)
    /** Put [value] over [range], combining with existing values via [remappingFunction] on overlaps. */
    fun merge(range: Range<K>, value: V, remappingFunction: (V, V) -> V?)
    fun putAll(rangeMap: RangeMap<K, V>)
    fun clear()
    fun remove(range: Range<K>)
    fun asMapOfRanges(): Map<Range<K>, V>
    fun asDescendingMapOfRanges(): Map<Range<K>, V>
    fun subRangeMap(range: Range<K>): RangeMap<K, V>
}
