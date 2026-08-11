package dev.guavakt.collect

/**
 * Guava-shaped bidirectional map backed by two linked hash maps (KMP).
 */
class HashBiMap<K, V> private constructor(
    private val delegate: MutableMap<K, V> = LinkedHashMap(),
    private val inverseDelegate: MutableMap<V, K> = LinkedHashMap(),
) : MutableMap<K, V>, BiMap<K, V> {
    private var inverseField: HashBiMap<V, K>? = null

    override val size: Int get() = delegate.size
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
    override fun containsValue(value: V): Boolean = inverseDelegate.containsKey(value)
    override fun get(key: K): V? = delegate[key]

    override fun put(key: K, value: V): V? {
        val hadKey = delegate.containsKey(key)
        val oldValue = delegate[key]
        if (hadKey && oldValue == value) return oldValue
        if (inverseDelegate.containsKey(value) && inverseDelegate[value] != key) {
            throw IllegalArgumentException("value already present: $value")
        }
        if (hadKey) inverseDelegate.remove(oldValue)
        inverseDelegate[value] = key
        return delegate.put(key, value)
    }

    override fun remove(key: K): V? {
        if (!delegate.containsKey(key)) return null
        val value = delegate.remove(key)
        inverseDelegate.remove(value)
        return value
    }

    override fun clear() {
        delegate.clear()
        inverseDelegate.clear()
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) put(k, v)
    }

    override fun forcePut(key: K, value: V): V? {
        val hadKey = delegate.containsKey(key)
        val oldValue = delegate[key]
        if (hadKey && oldValue == value) return oldValue
        val oldKey = inverseDelegate[value]
        if (inverseDelegate.containsKey(value)) delegate.remove(oldKey)
        if (hadKey) inverseDelegate.remove(oldValue)
        inverseDelegate[value] = key
        delegate[key] = value
        return oldValue
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val iterator = delegate.entries.iterator()
                var last: MutableMap.MutableEntry<K, V>? = null
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        val entry = iterator.next()
                        last = entry
                        return object : MutableMap.MutableEntry<K, V> {
                            override val key: K get() = entry.key
                            override val value: V get() = entry.value
                            override fun setValue(newValue: V): V {
                                val old = entry.value
                                if (old == newValue) return old
                                if (inverseDelegate.containsKey(newValue)) {
                                    throw IllegalArgumentException("value already present: $newValue")
                                }
                                inverseDelegate.remove(old)
                                entry.setValue(newValue)
                                inverseDelegate[newValue] = entry.key
                                return old
                            }
                        }
                    }
                    override fun remove() {
                        val entry = last ?: throw IllegalStateException()
                        inverseDelegate.remove(entry.value)
                        iterator.remove()
                        last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException()
            override fun clear() = this@HashBiMap.clear()
        }

    override val keys: MutableSet<K>
        get() = object : AbstractMutableSet<K>() {
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<K> {
                val entries = this@HashBiMap.entries.iterator()
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean = entries.hasNext()
                    override fun next(): K = entries.next().key
                    override fun remove() = entries.remove()
                }
            }
            override fun contains(element: K): Boolean = delegate.containsKey(element)
            override fun remove(element: K): Boolean {
                if (!delegate.containsKey(element)) return false
                this@HashBiMap.remove(element)
                return true
            }
            override fun add(element: K): Boolean = throw UnsupportedOperationException()
            override fun clear() = this@HashBiMap.clear()
        }

    override val values: MutableSet<V>
        get() = object : AbstractMutableSet<V>() {
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<V> {
                val entries = this@HashBiMap.entries.iterator()
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean = entries.hasNext()
                    override fun next(): V = entries.next().value
                    override fun remove() = entries.remove()
                }
            }
            override fun contains(element: V): Boolean = inverseDelegate.containsKey(element)
            override fun remove(element: V): Boolean {
                if (!inverseDelegate.containsKey(element)) return false
                val key = inverseDelegate[element]
                @Suppress("UNCHECKED_CAST")
                this@HashBiMap.remove(key as K)
                return true
            }
            override fun add(element: V): Boolean = throw UnsupportedOperationException()
            override fun clear() = this@HashBiMap.clear()
        }

    override fun inverse(): HashBiMap<V, K> {
        var inv = inverseField
        if (inv == null) {
            inv = HashBiMap(inverseDelegate, delegate)
            inv.inverseField = this
            inverseField = inv
        }
        return inv
    }

    companion object {
        fun <K, V> create(): HashBiMap<K, V> = HashBiMap()
        fun <K, V> create(map: Map<out K, V>): HashBiMap<K, V> {
            val b = create<K, V>()
            b.putAll(map)
            return b
        }
    }
}
