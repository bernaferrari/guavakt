package com.bernaferrari.guavakt.collect

/**
 * A [MapDifference] whose result maps iterate in the comparator order supplied to
 * [Maps.difference]. Kotlin common code has no `SortedMap`, so each result is exposed as an
 * ordered immutable [Map] snapshot instead.
 */
interface SortedMapDifference<K, V> : MapDifference<K, V> {
    override fun entriesOnlyOnLeft(): Map<K, V>
    override fun entriesOnlyOnRight(): Map<K, V>
    override fun entriesInCommon(): Map<K, V>
    override fun entriesDiffering(): Map<K, MapDifference.ValueDifference<V>>
}
