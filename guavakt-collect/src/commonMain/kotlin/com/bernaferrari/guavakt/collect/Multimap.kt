package com.bernaferrari.guavakt.collect

/**
 * Guava Multimap — keys map to zero or more values.
 * [get] returns a **live** [MutableCollection] view (mutate through the view).
 */
interface Multimap<K, V> {
    fun size(): Int
    fun isEmpty(): Boolean = size() == 0
    fun containsKey(key: Any?): Boolean
    fun containsValue(value: Any?): Boolean
    fun containsEntry(key: Any?, value: Any?): Boolean
    /** Live view of values for [key]; mutations affect the multimap. */
    operator fun get(key: K): MutableCollection<V>
    fun keySet(): Set<K>
    /** Keys with multiplicity = number of values for that key (Guava [keys]). */
    fun keys(): Multiset<K>
    fun values(): Collection<V>
    fun entries(): Collection<Map.Entry<K, V>>
    fun asMap(): Map<K, Collection<V>>
    fun put(key: K, value: V): Boolean
    fun putAll(key: K, values: Iterable<V>): Boolean
    fun putAll(multimap: Multimap<out K, out V>): Boolean
    fun remove(key: Any?, value: Any?): Boolean
    fun removeAll(key: Any?): Collection<V>
    fun replaceValues(key: K, values: Iterable<V>): Collection<V>
    fun clear()
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}
