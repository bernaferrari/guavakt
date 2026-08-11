package dev.guavakt.cache

/** Assigns an entry's non-negative weight for a maximum-weight cache. */
fun interface Weigher<K, V> {
    fun weigh(key: K, value: V): Int
}
