package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

/** Immutable set multimap preserving key encounter order and unique values per key. */
@GwtCompatible(serializable = true, emulated = true)
class ImmutableSetMultimap<K, V> private constructor(
    private val map: Map<K, MutableSet<V>>,
    private val sizeField: Int,
    private val valueComparator: Comparator<in V>?,
) : ImmutableMultimap<K, V>(), SetMultimap<K, V> {
    private var inverseView: ImmutableSetMultimap<V, K>? = null
    private val emptyValues: MutableSet<V> = valueComparator?.let { ImmutableSortedSet.copyOf(it, emptyList()) }
        ?: ImmutableSet.of()
    private val keySetView: Set<K> = unmodifiableMutableSet(map.keys)
    private val keysView: Multiset<K> = ImmutableMultiset.builder<K>().apply {
        map.forEach { (key, values) -> addCopies(key, values.size) }
    }.build()
    private val valuesView: Collection<V> = ImmutableList.copyOf(map.values.flatMap { it })
    private val entriesView: Set<Map.Entry<K, V>> = ImmutableSet.copyOf(buildList {
        map.forEach { (key, values) -> values.forEach { value -> add(Maps.immutableEntry(key, value)) } }
    })
    private val asMapView: Map<K, Set<V>> = unmodifiableMutableMap(map)

    override fun size(): Int = sizeField
    override fun containsKey(key: Any?): Boolean = map.containsKey(key)
    override fun containsValue(value: Any?): Boolean = map.values.any { value in it }
    override fun containsEntry(key: Any?, value: Any?): Boolean = map[key]?.contains(value) == true
    override fun get(key: K): MutableSet<V> = map[key] ?: emptyValues
    override fun keySet(): Set<K> = keySetView
    override fun keys(): Multiset<K> = keysView
    override fun values(): Collection<V> = valuesView
    override fun entries(): Set<Map.Entry<K, V>> = entriesView
    override fun asMap(): Map<K, Set<V>> = asMapView

    override fun removeAll(key: Any?): Set<V> = throw UnsupportedOperationException("ImmutableSetMultimap")
    override fun replaceValues(key: K, values: Iterable<V>): Set<V> =
        throw UnsupportedOperationException("ImmutableSetMultimap")

    override fun inverse(): ImmutableSetMultimap<V, K> {
        inverseView?.let { return it }
        if (isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return this as ImmutableSetMultimap<V, K>
        }
        val inverse = builder<V, K>().putAll(entriesView.map { Maps.immutableEntry(it.value, it.key) }).build()
        inverse.inverseView = this
        inverseView = inverse
        return inverse
    }

    override fun equals(other: Any?): Boolean = other === this || (other is Multimap<*, *> && asMap() == other.asMap())
    override fun hashCode(): Int = asMap().hashCode()
    override fun toString(): String = asMap().toString()

    class Builder<K, V> {
        private val map = LinkedHashMap<K, LinkedHashSet<V>>()
        private var keyComparator: Comparator<in K>? = null
        private var valueComparator: Comparator<in V>? = null

        fun put(key: K, value: V): Builder<K, V> = apply {
            map.getOrPut(nonNull(key, "key")) { LinkedHashSet() }.add(nonNull(value, "value"))
        }
        fun put(entry: Map.Entry<out K, V>): Builder<K, V> = put(entry.key, entry.value)
        fun putAll(key: K, values: Iterable<V>): Builder<K, V> = apply { values.forEach { put(key, it) } }
        fun putAll(key: K, vararg values: V): Builder<K, V> = putAll(key, values.asList())
        fun putAll(multimap: Multimap<out K, out V>): Builder<K, V> = apply {
            multimap.asMap().forEach { (key, values) -> putAll(key, values) }
        }
        fun putAll(entries: Iterable<Map.Entry<out K, V>>): Builder<K, V> = apply { entries.forEach(::put) }
        fun orderKeysBy(comparator: Comparator<in K>): Builder<K, V> = apply {
            keyComparator = Preconditions.checkNotNull(comparator)
        }
        fun orderValuesBy(comparator: Comparator<in V>): Builder<K, V> = apply {
            valueComparator = Preconditions.checkNotNull(comparator)
        }

        fun build(): ImmutableSetMultimap<K, V> {
            if (map.isEmpty()) return of()
            val entries = map.entries.toList().let { source ->
                keyComparator?.let { comparator -> source.sortedWith { a, b -> comparator.compare(a.key, b.key) } }
                    ?: source
            }
            val snapshot = LinkedHashMap<K, MutableSet<V>>(entries.size)
            var size = 0
            for ((key, values) in entries) {
                val immutableValues: MutableSet<V> = valueComparator?.let { ImmutableSortedSet.copyOf(it, values) }
                    ?: ImmutableSet.copyOf(values)
                if (immutableValues.isNotEmpty()) {
                    snapshot[key] = immutableValues
                    size += immutableValues.size
                }
            }
            return if (size == 0) of() else ImmutableSetMultimap(snapshot, size, valueComparator)
        }
    }

    companion object {
        private val EMPTY = ImmutableSetMultimap<Any, Any>(emptyMap(), 0, null)

        @Suppress("UNCHECKED_CAST")
        fun <K, V> of(): ImmutableSetMultimap<K, V> = EMPTY as ImmutableSetMultimap<K, V>
        fun <K, V> of(key: K, value: V): ImmutableSetMultimap<K, V> = builder<K, V>().put(key, value).build()
        fun <K, V> of(k1: K, v1: V, k2: K, v2: V): ImmutableSetMultimap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).build()
        fun <K, V> of(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V): ImmutableSetMultimap<K, V> =
            builder<K, V>().put(k1, v1).put(k2, v2).put(k3, v3).build()
        fun <K, V> builder(): Builder<K, V> = Builder()

        fun <K, V> copyOf(multimap: Multimap<out K, out V>): ImmutableSetMultimap<K, V> {
            if (multimap is ImmutableSetMultimap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return multimap as ImmutableSetMultimap<K, V>
            }
            if (multimap.isEmpty()) return of()
            return builder<K, V>().putAll(multimap).build()
        }

        fun <K, V> copyOf(entries: Iterable<Map.Entry<out K, V>>): ImmutableSetMultimap<K, V> =
            builder<K, V>().putAll(entries).build()

        private fun <T> nonNull(value: T, role: String): T = value ?: throw NullPointerException("null $role")
    }
}
