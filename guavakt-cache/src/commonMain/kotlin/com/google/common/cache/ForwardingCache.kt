package dev.guavakt.cache

/**
 * Guava ForwardingCache — forwards all calls to a delegate [Cache].
 */
abstract class ForwardingCache<K, V> : Cache<K, V> {
    protected abstract fun delegate(): Cache<K, V>

    override fun getIfPresent(key: K): V? = delegate().getIfPresent(key)
    override fun get(key: K, loader: () -> V): V = delegate().get(key, loader)
    override fun getAllPresent(keys: Iterable<K>): Map<K, V> = delegate().getAllPresent(keys)
    override fun put(key: K, value: V) = delegate().put(key, value)
    override fun putAll(m: Map<out K, V>) = delegate().putAll(m)
    override fun invalidate(key: K) = delegate().invalidate(key)
    override fun invalidateAll(keys: Iterable<K>) = delegate().invalidateAll(keys)
    override fun invalidateAll() = delegate().invalidateAll()
    override fun size(): Long = delegate().size()
    override fun stats(): CacheStats = delegate().stats()
    override fun asMap(): Map<K, V> = delegate().asMap()
    override fun cleanUp() = delegate().cleanUp()
}
