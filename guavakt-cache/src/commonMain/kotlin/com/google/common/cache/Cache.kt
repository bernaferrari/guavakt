package dev.guavakt.cache

interface Cache<K, V> {
    fun getIfPresent(key: K): V?
    fun get(key: K, loader: () -> V): V
    fun getAllPresent(keys: Iterable<K>): Map<K, V>
    fun put(key: K, value: V)
    fun putAll(m: Map<out K, V>)
    fun invalidate(key: K)
    fun invalidateAll(keys: Iterable<K>)
    fun invalidateAll()
    fun size(): Long
    fun stats(): CacheStats
    fun asMap(): Map<K, V>
    fun cleanUp()
}
