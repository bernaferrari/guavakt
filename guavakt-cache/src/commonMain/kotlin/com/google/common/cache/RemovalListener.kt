package dev.guavakt.cache

/** Receives a cache removal after its entry is detached from the cache. */
fun interface RemovalListener<K, V> {
    fun onRemoval(notification: RemovalNotification<K, V>)
}
