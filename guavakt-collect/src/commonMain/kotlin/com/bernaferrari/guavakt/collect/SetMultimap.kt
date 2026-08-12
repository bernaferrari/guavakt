package com.bernaferrari.guavakt.collect

interface SetMultimap<K, V> : Multimap<K, V> {
    override fun get(key: K): MutableSet<V>
    override fun removeAll(key: Any?): Set<V>
    override fun replaceValues(key: K, values: Iterable<V>): Set<V>
    override fun asMap(): Map<K, Set<V>>
}
