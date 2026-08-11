package dev.guavakt.collect

/**
 * Guava ImmutableBiMap — immutable bidirectional map.
 */
class ImmutableBiMap<K, V> private constructor(
    private val delegate: Map<K, V>,
    private val inverseMap: Map<V, K>,
) : BiMap<K, V> {
    private var inverseView: ImmutableBiMap<V, K>? = null
    private val entryView = immutableSetView(delegate.map { (key, value) -> Maps.immutableEntry(key, value) })
    private val keyView = immutableSetView(delegate.keys)
    private val valueView = immutableSetView(inverseMap.keys)
    override val entries: Set<Map.Entry<K, V>> get() = entryView
    override val keys: Set<K> get() = keyView
    override val values: Set<V> get() = valueView
    override val size: Int get() = delegate.size
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun get(key: K): V? = delegate[key]
    override fun containsKey(key: K): Boolean = delegate.containsKey(key)
    override fun containsValue(value: V): Boolean = inverseMap.containsKey(value)

    override fun inverse(): ImmutableBiMap<V, K> {
        inverseView?.let { return it }
        val reversed = ImmutableBiMap(inverseMap, delegate)
        reversed.inverseView = this
        inverseView = reversed
        return reversed
    }

    override fun forcePut(key: K, value: V): V? =
        throw UnsupportedOperationException("ImmutableBiMap")

    private fun <E> immutableSetView(elements: Iterable<E>): Set<E> =
        object : AbstractMutableSet<E>() {
            private val snapshot = LinkedHashSet<E>().apply { addAll(elements) }
            override val size: Int get() = snapshot.size
            override fun contains(element: E): Boolean = snapshot.contains(element)
            override fun iterator(): MutableIterator<E> {
                val iterator = snapshot.iterator()
                return object : MutableIterator<E> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): E = iterator.next()
                    override fun remove(): Unit = throw UnsupportedOperationException("ImmutableBiMap")
                }
            }
            override fun add(element: E): Boolean = throw UnsupportedOperationException("ImmutableBiMap")
            override fun remove(element: E): Boolean = throw UnsupportedOperationException("ImmutableBiMap")
            override fun clear(): Unit = throw UnsupportedOperationException("ImmutableBiMap")
        }

    companion object {
        private val EMPTY = ImmutableBiMap<Any?, Any?>(emptyMap(), emptyMap())

        @Suppress("UNCHECKED_CAST")
        fun <K, V> of(): ImmutableBiMap<K, V> = EMPTY as ImmutableBiMap<K, V>

        fun <K, V> of(k1: K, v1: V): ImmutableBiMap<K, V> =
            ImmutableBiMap(mapOf(nonNull(k1, "key") to nonNull(v1, "value")), mapOf(v1 to k1))

        fun <K, V> of(k1: K, v1: V, k2: K, v2: V): ImmutableBiMap<K, V> {
            nonNull(k1, "key")
            nonNull(v1, "value")
            nonNull(k2, "key")
            nonNull(v2, "value")
            require(k1 != k2 && v1 != v2)
            return ImmutableBiMap(
                linkedMapOf(k1 to v1, k2 to v2),
                linkedMapOf(v1 to k1, v2 to k2),
            )
        }

        fun <K, V> copyOf(map: Map<out K, V>): ImmutableBiMap<K, V> {
            if (map is ImmutableBiMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return map as ImmutableBiMap<K, V>
            }
            if (map.isEmpty()) return of()
            val forward = LinkedHashMap<K, V>()
            val backward = LinkedHashMap<V, K>()
            for ((k, v) in map) {
                nonNull(k, "key")
                nonNull(v, "value")
                require(k !in forward) { "duplicate key" }
                require(v !in backward) { "duplicate value" }
                forward[k] = v
                backward[v] = k
            }
            return ImmutableBiMap(forward, backward)
        }

        fun <K, V> builder(): Builder<K, V> = Builder()

        class Builder<K, V> {
            private val entries = LinkedHashMap<K, V>()
            fun put(key: K, value: V): Builder<K, V> = apply {
                nonNull(key, "key")
                nonNull(value, "value")
                require(key !in entries)
                require(value !in entries.values)
                entries[key] = value
            }
            fun putAll(map: Map<out K, V>): Builder<K, V> = apply {
                for ((k, v) in map) put(k, v)
            }
            fun build(): ImmutableBiMap<K, V> = copyOf(entries)
        }

        private fun <K, V> linkedMapOf(vararg pairs: Pair<K, V>): Map<K, V> =
            LinkedHashMap<K, V>().apply { for (p in pairs) put(p.first, p.second) }

        private fun <T> nonNull(value: T, role: String): T =
            value ?: throw NullPointerException("null $role")
    }
}
