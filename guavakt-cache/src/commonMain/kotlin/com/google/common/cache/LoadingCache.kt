package dev.guavakt.cache

interface LoadingCache<K, V> : Cache<K, V> {
    fun get(key: K): V

    /**
     * Resolves each distinct key once, preserving first-encounter order.
     *
     * When [CacheLoader.loadAll] is overridden, all initially missing keys are offered to it in a
     * single ordered unique batch. The default loader implementation falls back to individual
     * [get] calls, preserving Guava's request and load-statistics behavior on every KMP target.
     */
    fun getAll(keys: Iterable<K>): Map<K, V>
    fun refresh(key: K)
}
