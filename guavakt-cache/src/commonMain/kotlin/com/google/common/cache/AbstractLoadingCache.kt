package dev.guavakt.cache

/**
 * Guava-style skeletal [LoadingCache] base.
 *
 * Subclasses provide [get] and [getIfPresent]; [getAll] deduplicates keys in first-encounter
 * order. Storage operations retain [AbstractCache]'s unsupported defaults until a subclass opts
 * into them, and [refresh] is unsupported by default.
 */
abstract class AbstractLoadingCache<K, V> : AbstractCache<K, V>(), LoadingCache<K, V> {
    override fun getAll(keys: Iterable<K>): Map<K, V> {
        val result = LinkedHashMap<K, V>()
        for (key in keys) {
            if (key !in result) result[key] = get(key)
        }
        return result
    }

    override fun refresh(key: K): Unit = throw UnsupportedOperationException()
}
