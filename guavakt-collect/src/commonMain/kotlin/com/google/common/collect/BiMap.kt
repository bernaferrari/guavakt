package dev.guavakt.collect

/**
 * Guava BiMap — map that preserves the uniqueness of its values as well as keys.
 * Extends Map (not MutableMap) so immutable implementations can conform.
 */
interface BiMap<K, V> : Map<K, V> {
    override val values: Set<V>
    fun inverse(): BiMap<V, K>
    /** Mutable BiMaps implement put; immutable throws. */
    fun forcePut(key: K, value: V): V?
}
