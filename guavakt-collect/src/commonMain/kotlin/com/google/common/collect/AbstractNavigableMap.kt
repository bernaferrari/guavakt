package dev.guavakt.collect

/**
 * Guava AbstractNavigableMap — sorted map base with navigable key ops (portable ComparatorTreeMap).
 */
open class AbstractNavigableMap<K, V> protected constructor(
    private val cmp: Comparator<in K>? = null,
    protected val delegate: ComparatorTreeMap<K, V> = ComparatorTreeMap(cmp),
) : AbstractMutableMap<K, V>() {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = delegate.entries
    override val size: Int get() = delegate.size
    override fun get(key: K): V? = delegate[key]
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
    override fun put(key: K, value: V): V? = delegate.put(key, value)
    override fun remove(key: K): V? = delegate.remove(key)
    override fun clear() = delegate.clear()

    open fun firstKey(): K = delegate.firstKey()
    open fun lastKey(): K = delegate.lastKey()
    open fun lowerKey(key: K): K? {
        val keys = delegate.keys.toList()
        return keys.lastOrNull { compareKeys(it, key) < 0 }
    }
    open fun floorKey(key: K): K? {
        val keys = delegate.keys.toList()
        return keys.lastOrNull { compareKeys(it, key) <= 0 }
    }
    open fun ceilingKey(key: K): K? {
        val keys = delegate.keys.toList()
        return keys.firstOrNull { compareKeys(it, key) >= 0 }
    }
    open fun higherKey(key: K): K? {
        val keys = delegate.keys.toList()
        return keys.firstOrNull { compareKeys(it, key) > 0 }
    }

    private fun compareKeys(a: K, b: K): Int =
        if (cmp != null) cmp.compare(a, b)
        else {
            @Suppress("UNCHECKED_CAST")
            (a as Comparable<K>).compareTo(b)
        }

    companion object {
        fun <K : Comparable<K>, V> create(): AbstractNavigableMap<K, V> = AbstractNavigableMap(null)
        fun <K, V> create(comparator: Comparator<in K>): AbstractNavigableMap<K, V> =
            AbstractNavigableMap(comparator)
        fun <K : Comparable<K>, V> create(map: Map<out K, V>): AbstractNavigableMap<K, V> =
            AbstractNavigableMap<K, V>(null).also { it.putAll(map) }
        fun <K : Comparable<K>, V> createWithExpectedSize(expectedSize: Int): AbstractNavigableMap<K, V> =
            AbstractNavigableMap(null)
    }
}
