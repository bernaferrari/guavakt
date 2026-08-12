package com.bernaferrari.guavakt.cache

/** Loads a cache value without blocking the calling thread. */
fun interface SuspendingCacheLoader<K, V> {
    suspend fun load(key: K): V
}
