package com.bernaferrari.guavakt.cache

/**
 * Guava AbstractCache — skeletal [Cache] implementation; subclasses implement storage.
 */
abstract class AbstractCache<K, V> : Cache<K, V> {
    override fun get(key: K, loader: () -> V): V = throw UnsupportedOperationException()

    override fun getAllPresent(keys: Iterable<K>): Map<K, V> {
        val result = LinkedHashMap<K, V>()
        for (key in keys) {
            if (key !in result) getIfPresent(key)?.let { result[key] = it }
        }
        return result
    }

    override fun put(key: K, value: V): Unit = throw UnsupportedOperationException()

    override fun putAll(m: Map<out K, V>) {
        for ((key, value) in m) put(key, value)
    }

    override fun invalidate(key: K): Unit = throw UnsupportedOperationException()

    override fun invalidateAll(keys: Iterable<K>) {
        for (key in keys) invalidate(key)
    }

    override fun invalidateAll(): Unit = throw UnsupportedOperationException()

    override fun size(): Long = throw UnsupportedOperationException()

    override fun stats(): CacheStats = throw UnsupportedOperationException()

    override fun asMap(): Map<K, V> = throw UnsupportedOperationException()

    override fun cleanUp() {}
}
