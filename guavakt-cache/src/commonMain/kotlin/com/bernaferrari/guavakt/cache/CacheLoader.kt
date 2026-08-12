package com.bernaferrari.guavakt.cache

fun interface CacheLoader<K, V> {
    fun load(key: K): V

    /**
     * Computes values for the distinct missing [keys] requested by [LoadingCache.getAll].
     *
     * The default deliberately signals that no batch implementation is available, after which the
     * cache falls back to [load] once per key. A custom result may contain extra entries; they are
     * cached, while `getAll` returns only its requested keys. Null keys, values, or a missing
     * requested key are rejected after valid returned entries have been cached, matching Guava.
     */
    fun loadAll(keys: Iterable<K>): Map<K, V> = throw UnsupportedLoadingOperationException()

    /** Signals that [loadAll] intentionally falls back to individual [load] calls. */
    class UnsupportedLoadingOperationException : UnsupportedOperationException()

    /** Signals a null, incomplete, or otherwise invalid result from [loadAll]. */
    class InvalidCacheLoadException(message: String) : IllegalStateException(message)
}
