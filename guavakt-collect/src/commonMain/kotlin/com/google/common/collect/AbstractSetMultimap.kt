package dev.guavakt.collect

/**
 * Guava AbstractSetMultimap — [get] returns a live [Set] view; put may no-op on duplicate values.
 */
abstract class AbstractSetMultimap<K, V> protected constructor(
    map: MutableMap<K, MutableCollection<V>>,
) : AbstractMapBasedMultimap<K, V>(map), SetMultimap<K, V> {

    protected abstract override fun createCollection(): MutableSet<V>

    override fun get(key: K): MutableSet<V> {
        @Suppress("UNCHECKED_CAST")
        return super.get(key) as MutableSet<V>
    }

    override fun removeAll(key: Any?): Set<V> {
        @Suppress("UNCHECKED_CAST")
        return super.removeAll(key) as Set<V>
    }

    override fun replaceValues(key: K, values: Iterable<V>): Set<V> {
        @Suppress("UNCHECKED_CAST")
        return super.replaceValues(key, values) as Set<V>
    }

    override fun asMap(): Map<K, Set<V>> {
        @Suppress("UNCHECKED_CAST")
        return super.asMap() as Map<K, Set<V>>
    }

    override fun entries(): Set<Map.Entry<K, V>> {
        val entries = super.entries()
        return object : AbstractMutableSet<Map.Entry<K, V>>() {
            override val size: Int get() = entries.size
            override fun iterator(): MutableIterator<Map.Entry<K, V>> =
                entries.iterator() as MutableIterator<Map.Entry<K, V>>
            override fun contains(element: Map.Entry<K, V>): Boolean = entries.contains(element)
            override fun remove(element: Map.Entry<K, V>): Boolean =
                this@AbstractSetMultimap.remove(element.key, element.value)
            override fun clear() = this@AbstractSetMultimap.clear()
            override fun add(element: Map.Entry<K, V>): Boolean = throw UnsupportedOperationException()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is SetMultimap<*, *>) return false
        return super.equals(other)
    }
}
