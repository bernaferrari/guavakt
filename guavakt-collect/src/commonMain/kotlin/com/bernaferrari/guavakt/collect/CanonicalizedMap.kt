package com.bernaferrari.guavakt.collect

/**
 * A map that stores entries by a canonical form of their keys while retaining the most recently
 * supplied original key for iteration. This is useful for case-insensitive protocol keys without
 * repeatedly normalizing every stored key during lookup.
 */
class CanonicalizedMap<C, K, V>(
    private val canonicalize: (K) -> C,
) : AbstractMutableMap<K, V>() {
    private data class StoredEntry<K, V>(var key: K, var value: V)
    private val entriesByCanonicalKey = LinkedHashMap<C, StoredEntry<K, V>>()

    override val size: Int get() = entriesByCanonicalKey.size
    override fun get(key: K): V? = entriesByCanonicalKey[canonicalize(key)]?.value
    override fun containsKey(key: K): Boolean = entriesByCanonicalKey.containsKey(canonicalize(key))

    override fun put(key: K, value: V): V? {
        val canonicalKey = canonicalize(key)
        val existing = entriesByCanonicalKey[canonicalKey]
        if (existing == null) {
            entriesByCanonicalKey[canonicalKey] = StoredEntry(key, value)
            return null
        }
        val oldValue = existing.value
        existing.key = key
        existing.value = value
        return oldValue
    }

    override fun remove(key: K): V? = entriesByCanonicalKey.remove(canonicalize(key))?.value

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = this@CanonicalizedMap.size
        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
            this@CanonicalizedMap[element.key] == element.value && this@CanonicalizedMap.containsKey(element.key)

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            if (!contains(element)) return false
            this@CanonicalizedMap.remove(element.key)
            return true
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val iterator = entriesByCanonicalKey.entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableMap.MutableEntry<K, V> {
                    val stored = iterator.next().value
                    return object : MutableMap.MutableEntry<K, V> {
                        override val key: K get() = stored.key
                        override val value: V get() = stored.value
                        override fun setValue(newValue: V): V {
                            val oldValue = stored.value
                            stored.value = newValue
                            return oldValue
                        }

                        override fun equals(other: Any?): Boolean =
                            other is Map.Entry<*, *> && key == other.key && value == other.value

                        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
                    }
                }

                override fun remove() = iterator.remove()
            }
        }

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            val present = this@CanonicalizedMap.containsKey(element.key)
            this@CanonicalizedMap[element.key] = element.value
            return !present
        }
    }

    override val keys: MutableSet<K> = object : AbstractMutableSet<K>() {
        override val size: Int get() = this@CanonicalizedMap.size
        override fun contains(element: K): Boolean = this@CanonicalizedMap.containsKey(element)
        override fun add(element: K): Boolean = throw UnsupportedOperationException("keys view does not support add")
        override fun remove(element: K): Boolean {
            if (!this@CanonicalizedMap.containsKey(element)) return false
            this@CanonicalizedMap.remove(element)
            return true
        }

        override fun iterator(): MutableIterator<K> {
            val iterator = this@CanonicalizedMap.entries.iterator()
            return object : MutableIterator<K> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): K = iterator.next().key
                override fun remove() = iterator.remove()
            }
        }

        override fun clear() = this@CanonicalizedMap.clear()
    }

    override fun clear() = entriesByCanonicalKey.clear()
}
