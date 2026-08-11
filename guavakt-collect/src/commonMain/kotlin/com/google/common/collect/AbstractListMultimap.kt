package dev.guavakt.collect

/**
 * Guava AbstractListMultimap — [get] returns a live [List] view.
 */
abstract class AbstractListMultimap<K, V> protected constructor(
    map: MutableMap<K, MutableCollection<V>>,
) : AbstractMapBasedMultimap<K, V>(map), ListMultimap<K, V> {

    protected abstract override fun createCollection(): MutableList<V>

    override fun get(key: K): MutableList<V> {
        @Suppress("UNCHECKED_CAST")
        return super.get(key) as MutableList<V>
    }

    override fun removeAll(key: Any?): List<V> {
        @Suppress("UNCHECKED_CAST")
        return super.removeAll(key) as List<V>
    }

    override fun replaceValues(key: K, values: Iterable<V>): List<V> {
        @Suppress("UNCHECKED_CAST")
        return super.replaceValues(key, values) as List<V>
    }

    override fun asMap(): Map<K, List<V>> {
        @Suppress("UNCHECKED_CAST")
        return super.asMap() as Map<K, List<V>>
    }

    override fun put(key: K, value: V): Boolean = super.put(key, value)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ListMultimap<*, *>) return false
        return super.equals(other)
    }
}
