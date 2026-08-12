package com.bernaferrari.guavakt.collect

/**
 * Guava AbstractBiMap — bidirectional map keeping inverse in sync.
 */
abstract class AbstractBiMap<K, V> protected constructor(
    private val delegate: MutableMap<K, V>,
    private val inverseDelegate: MutableMap<V, K>,
) : AbstractMutableMap<K, V>(), BiMap<K, V> {
    private var inverseField: AbstractBiMap<V, K>? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val it = delegate.entries.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var last: MutableMap.MutableEntry<K, V>? = null
                    override fun hasNext() = it.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        val e = it.next()
                        last = e
                        return object : MutableMap.MutableEntry<K, V> {
                            override val key: K get() = e.key
                            override val value: V get() = e.value
                            override fun setValue(newValue: V): V {
                                val old = e.value
                                putInBoth(e.key, newValue, force = false)
                                return old
                            }
                        }
                    }
                    override fun remove() {
                        val e = last ?: throw IllegalStateException()
                        inverseDelegate.remove(e.value)
                        it.remove()
                        last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException()
            override fun clear() = this@AbstractBiMap.clear()
        }

    override val size: Int get() = delegate.size

    override fun get(key: K): V? = delegate[key]

    override fun containsKey(key: K): Boolean = delegate.containsKey(key)

    override fun containsValue(value: V): Boolean = inverseDelegate.containsKey(value)

    override val values: MutableSet<V>
        get() = object : AbstractMutableSet<V>() {
            override val size: Int get() = inverseDelegate.size
            override fun contains(element: V): Boolean = inverseDelegate.containsKey(element)
            override fun iterator(): MutableIterator<V> {
                val entries = this@AbstractBiMap.entries.iterator()
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean = entries.hasNext()
                    override fun next(): V = entries.next().value
                    override fun remove() = entries.remove()
                }
            }
            override fun add(element: V): Boolean = throw UnsupportedOperationException()
            override fun remove(element: V): Boolean {
                if (!inverseDelegate.containsKey(element)) return false
                val key = inverseDelegate[element]
                @Suppress("UNCHECKED_CAST")
                this@AbstractBiMap.remove(key as K)
                return true
            }
            override fun clear() = this@AbstractBiMap.clear()
        }

    override fun put(key: K, value: V): V? = putInBoth(key, value, false)

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) put(k, v)
    }

    override fun forcePut(key: K, value: V): V? = putInBoth(key, value, true)

    private fun putInBoth(key: K, value: V, force: Boolean): V? {
        val hadKey = delegate.containsKey(key)
        val oldValue = delegate[key]
        if (hadKey && oldValue == value) return oldValue
        val hadValue = inverseDelegate.containsKey(value)
        val oldKeyForValue = inverseDelegate[value]
        if (hadValue) {
            if (!force && (!hadKey || oldKeyForValue != key)) {
                throw IllegalArgumentException("value already present: $value")
            }
            delegate.remove(oldKeyForValue)
        }
        if (hadKey) inverseDelegate.remove(oldValue)
        delegate[key] = value
        inverseDelegate[value] = key
        return oldValue
    }

    override fun remove(key: K): V? {
        if (!delegate.containsKey(key)) return null
        @Suppress("UNCHECKED_CAST")
        val value = delegate.remove(key) as V
        inverseDelegate.remove(value)
        return value
    }

    override fun clear() {
        delegate.clear()
        inverseDelegate.clear()
    }

    override fun inverse(): BiMap<V, K> {
        var inv = inverseField
        if (inv == null) {
            inv = Inverse(this)
            inverseField = inv
        }
        return inv
    }

    private class Inverse<K, V>(
        private val forward: AbstractBiMap<V, K>,
    ) : AbstractBiMap<K, V>(forward.inverseDelegate, forward.delegate) {
        override fun inverse(): BiMap<V, K> = forward
    }

    companion object {
        fun <K, V> create(): AbstractBiMap<K, V> =
            object : AbstractBiMap<K, V>(LinkedHashMap(), LinkedHashMap()) {}
    }
}
