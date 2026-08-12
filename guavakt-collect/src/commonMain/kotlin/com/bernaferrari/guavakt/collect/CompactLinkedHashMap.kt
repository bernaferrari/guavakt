package com.bernaferrari.guavakt.collect

/**
 * Guava CompactLinkedHashMap — insertion-ordered map (portable LinkedHashMap core + Guava factories).
 * Iteration order is insertion order (Guava linked compact map contract).
 */
open class CompactLinkedHashMap<K, V> private constructor(
    private val map: LinkedHashMap<K, V>,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = map.size
    override fun get(key: K): V? = map[key]
    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun put(key: K, value: V): V? = map.put(key, value)
    override fun remove(key: K): V? = map.remove(key)
    override fun clear() = map.clear()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries

    /** Guava API — no-op on LinkedHashMap storage (capacity managed by platform map). */
    fun trimToSize() { /* LinkedHashMap has no public capacity trim; API preserved */ }

    companion object {
        fun <K, V> create(): CompactLinkedHashMap<K, V> = CompactLinkedHashMap(LinkedHashMap())
        fun <K, V> create(expectedSize: Int): CompactLinkedHashMap<K, V> =
            CompactLinkedHashMap(LinkedHashMap(expectedSize.coerceAtLeast(0) * 4 / 3 + 1))
        fun <K, V> createWithExpectedSize(expectedSize: Int): CompactLinkedHashMap<K, V> = create(expectedSize)
        fun <K, V> create(map: Map<out K, V>): CompactLinkedHashMap<K, V> =
            CompactLinkedHashMap(LinkedHashMap(map))
    }
}
