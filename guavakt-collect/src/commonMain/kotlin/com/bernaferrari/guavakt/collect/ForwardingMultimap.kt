package com.bernaferrari.guavakt.collect

/** Guava ForwardingMultimap — forwards multimap operations to [delegate]. */
abstract class ForwardingMultimap<K, V> : Multimap<K, V> {
    protected abstract fun delegate(): Multimap<K, V>
    override fun size(): Int = delegate().size()
    override fun isEmpty(): Boolean = delegate().isEmpty()
    override fun containsKey(key: Any?): Boolean = delegate().containsKey(key)
    override fun containsValue(value: Any?): Boolean = delegate().containsValue(value)
    override fun containsEntry(key: Any?, value: Any?): Boolean = delegate().containsEntry(key, value)
    override fun get(key: K): MutableCollection<V> = delegate().get(key)
    override fun keySet(): Set<K> = delegate().keySet()
    override fun keys(): Multiset<K> = delegate().keys()
    override fun values(): Collection<V> = delegate().values()
    override fun entries(): Collection<Map.Entry<K, V>> = delegate().entries()
    override fun asMap(): Map<K, Collection<V>> = delegate().asMap()
    override fun put(key: K, value: V): Boolean = delegate().put(key, value)
    override fun remove(key: Any?, value: Any?): Boolean = delegate().remove(key, value)
    override fun putAll(key: K, values: Iterable<V>): Boolean = delegate().putAll(key, values)
    override fun putAll(multimap: Multimap<out K, out V>): Boolean = delegate().putAll(multimap)
    override fun replaceValues(key: K, values: Iterable<V>): Collection<V> = delegate().replaceValues(key, values)
    override fun removeAll(key: Any?): Collection<V> = delegate().removeAll(key)
    override fun clear() = delegate().clear()
    override fun equals(other: Any?): Boolean = delegate() == other
    override fun hashCode(): Int = delegate().hashCode()
}
