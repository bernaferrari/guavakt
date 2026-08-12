package com.bernaferrari.guavakt.collect

interface ListMultimap<K, V> : Multimap<K, V> {
    override fun get(key: K): MutableList<V>
    override fun removeAll(key: Any?): List<V>
    override fun replaceValues(key: K, values: Iterable<V>): List<V>
    override fun asMap(): Map<K, List<V>>
}
