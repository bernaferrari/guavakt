package dev.guavakt.collect

/**
 * Guava ForwardingMap — forwards all map calls to [delegate].
 */
abstract class ForwardingMap<K, V> : AbstractMutableMap<K, V>() {
    protected abstract fun delegate(): MutableMap<K, V>

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = delegate().entries
    override val keys: MutableSet<K> get() = delegate().keys
    override val values: MutableCollection<V> get() = delegate().values
    override val size: Int get() = delegate().size
    override fun isEmpty(): Boolean = delegate().isEmpty()
    override fun containsKey(key: K): Boolean = delegate().containsKey(key)
    override fun containsValue(value: V): Boolean = delegate().containsValue(value)
    override fun get(key: K): V? = delegate()[key]
    override fun put(key: K, value: V): V? = delegate().put(key, value)
    override fun remove(key: K): V? = delegate().remove(key)
    override fun putAll(from: Map<out K, V>) = delegate().putAll(from)
    override fun clear() = delegate().clear()
}
