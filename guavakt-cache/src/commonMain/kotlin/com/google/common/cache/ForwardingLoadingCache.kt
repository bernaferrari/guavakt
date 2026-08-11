package dev.guavakt.cache

abstract class ForwardingLoadingCache<K, V> : ForwardingCache<K, V>(), LoadingCache<K, V> {
    abstract override fun delegate(): LoadingCache<K, V>
    override fun get(key: K): V = delegate().get(key)
    override fun getAll(keys: Iterable<K>): Map<K, V> = delegate().getAll(keys)
    override fun refresh(key: K) = delegate().refresh(key)
}
