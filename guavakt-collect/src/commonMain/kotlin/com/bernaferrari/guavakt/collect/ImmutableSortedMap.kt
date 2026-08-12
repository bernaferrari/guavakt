package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Immutable comparator-backed map whose iteration order is key order.
 *
 * Comparator equivalence defines key identity. Kotlin common code has no `NavigableMap`, so range
 * and navigation operations are exposed directly on this type.
 */
class ImmutableSortedMap<K, V> private constructor(
    private val orderedEntries: List<Pair<K, V>>,
    private val ordering: Comparator<in K>,
) : AbstractMutableMap<K, V>() {
    private var descendingView: ImmutableSortedMap<K, V>? = null
    private val orderedKeys = orderedEntries.map { it.first }

    override val size: Int get() = orderedEntries.size

    override fun containsKey(key: K): Boolean {
        if (key == null) return false
        return try {
            indexOf(key) >= 0
        } catch (_: ClassCastException) {
            false
        }
    }

    override fun get(key: K): V? {
        if (key == null) return null
        return try {
            indexOf(key).takeIf { it >= 0 }?.let { orderedEntries[it].second }
        } catch (_: ClassCastException) {
            null
        }
    }

    override fun containsValue(value: V): Boolean = orderedEntries.any { it.second == value }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> =
        object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            private val snapshot = orderedEntries.map { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                Maps.immutableEntry(key, value) as MutableMap.MutableEntry<K, V>
            }

            override val size: Int get() = snapshot.size
            override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
                this@ImmutableSortedMap[element.key] == element.value &&
                    (element.value != null || this@ImmutableSortedMap.containsKey(element.key))

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val iterator = snapshot.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> = iterator.next()
                    override fun remove(): Unit = throw UnsupportedOperationException("ImmutableSortedMap")
                }
            }

            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException("ImmutableSortedMap")
            override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean =
                throw UnsupportedOperationException("ImmutableSortedMap")
            override fun clear(): Unit = throw UnsupportedOperationException("ImmutableSortedMap")
        }

    override fun put(key: K, value: V): V? = throw UnsupportedOperationException("ImmutableSortedMap")
    override fun remove(key: K): V? = throw UnsupportedOperationException("ImmutableSortedMap")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableSortedMap")

    fun comparator(): Comparator<in K> = ordering
    fun firstKey(): K = orderedEntries.first().first
    fun lastKey(): K = orderedEntries.last().first

    fun lowerKey(key: K): K? = keyAtOrNull(lowerBound(nonNull(key, "key")) - 1)
    fun floorKey(key: K): K? = keyAtOrNull(upperBound(nonNull(key, "key")) - 1)
    fun ceilingKey(key: K): K? = keyAtOrNull(lowerBound(nonNull(key, "key")))
    fun higherKey(key: K): K? = keyAtOrNull(upperBound(nonNull(key, "key")))

    fun firstEntry(): Map.Entry<K, V>? = entryAtOrNull(0)
    fun lastEntry(): Map.Entry<K, V>? = entryAtOrNull(size - 1)
    fun pollFirstEntry(): Map.Entry<K, V>? = throw UnsupportedOperationException("ImmutableSortedMap")
    fun pollLastEntry(): Map.Entry<K, V>? = throw UnsupportedOperationException("ImmutableSortedMap")
    fun lowerEntry(key: K): Map.Entry<K, V>? = entryAtOrNull(lowerBound(nonNull(key, "key")) - 1)
    fun floorEntry(key: K): Map.Entry<K, V>? = entryAtOrNull(upperBound(nonNull(key, "key")) - 1)
    fun ceilingEntry(key: K): Map.Entry<K, V>? = entryAtOrNull(lowerBound(nonNull(key, "key")))
    fun higherEntry(key: K): Map.Entry<K, V>? = entryAtOrNull(upperBound(nonNull(key, "key")))

    fun headMap(toKey: K): ImmutableSortedMap<K, V> = headMap(toKey, false)

    fun headMap(toKey: K, inclusive: Boolean): ImmutableSortedMap<K, V> =
        slice(0, if (inclusive) upperBound(nonNull(toKey, "key")) else lowerBound(nonNull(toKey, "key")))

    fun tailMap(fromKey: K): ImmutableSortedMap<K, V> = tailMap(fromKey, true)

    fun tailMap(fromKey: K, inclusive: Boolean): ImmutableSortedMap<K, V> =
        slice(if (inclusive) lowerBound(nonNull(fromKey, "key")) else upperBound(nonNull(fromKey, "key")), size)

    fun subMap(fromKey: K, toKey: K): ImmutableSortedMap<K, V> = subMap(fromKey, true, toKey, false)

    fun subMap(
        fromKey: K,
        fromInclusive: Boolean,
        toKey: K,
        toInclusive: Boolean,
    ): ImmutableSortedMap<K, V> {
        val from = nonNull(fromKey, "key")
        val to = nonNull(toKey, "key")
        require(ordering.compare(from, to) <= 0) { "expected fromKey <= toKey" }
        val lower = if (fromInclusive) lowerBound(from) else upperBound(from)
        val upper = if (toInclusive) upperBound(to) else lowerBound(to)
        return slice(lower.coerceAtMost(upper), upper)
    }

    fun descendingMap(): ImmutableSortedMap<K, V> {
        descendingView?.let { return it }
        val reversed = ImmutableSortedMap(
            orderedEntries.asReversed(),
            Comparator { a, b -> ordering.compare(b, a) },
        )
        reversed.descendingView = this
        descendingView = reversed
        return reversed
    }

    fun navigableKeySet(): ImmutableSortedSet<K> = ImmutableSortedSet.copyOf(ordering, orderedEntries.map { it.first })
    fun descendingKeySet(): ImmutableSortedSet<K> = descendingMap().navigableKeySet()

    private fun slice(fromIndex: Int, toIndex: Int): ImmutableSortedMap<K, V> = when {
        fromIndex == 0 && toIndex == size -> this
        fromIndex == toIndex -> emptyFor(ordering)
        else -> ImmutableSortedMap(orderedEntries.subList(fromIndex, toIndex), ordering)
    }

    private fun keyAtOrNull(index: Int): K? = orderedEntries.getOrNull(index)?.first

    private fun entryAtOrNull(index: Int): Map.Entry<K, V>? = orderedEntries.getOrNull(index)?.let { (key, value) ->
        Maps.immutableEntry(key, value)
    }

    private fun indexOf(key: K): Int = SortedLists.binarySearch(
        orderedKeys, key, ordering,
        SortedLists.KeyPresentBehavior.ANY_PRESENT,
        SortedLists.KeyAbsentBehavior.INVERTED_INSERTION_INDEX,
    )

    private fun lowerBound(key: K): Int = SortedLists.binarySearch(
        orderedKeys, key, ordering,
        SortedLists.KeyPresentBehavior.FIRST_PRESENT,
        SortedLists.KeyAbsentBehavior.NEXT_HIGHER,
    )

    private fun upperBound(key: K): Int = SortedLists.binarySearch(
        orderedKeys, key, ordering,
        SortedLists.KeyPresentBehavior.FIRST_AFTER,
        SortedLists.KeyAbsentBehavior.NEXT_HIGHER,
    )

    class Builder<K, V>(private val ordering: Comparator<in K>) {
        private val pending = ArrayList<Pair<K, V>>()

        fun put(key: K, value: V): Builder<K, V> = apply {
            pending.add(nonNull(key, "key") to nonNull(value, "value"))
        }

        fun put(entry: Map.Entry<out K, V>): Builder<K, V> = put(entry.key, entry.value)
        fun putAll(other: Map<out K, V>): Builder<K, V> = apply { other.forEach { (key, value) -> put(key, value) } }
        fun putAll(entries: Iterable<Map.Entry<out K, V>>): Builder<K, V> = apply { entries.forEach(::put) }

        fun build(): ImmutableSortedMap<K, V> = buildOrThrow()

        fun buildOrThrow(): ImmutableSortedMap<K, V> {
            if (pending.isEmpty()) return emptyFor(ordering)
            val sorted = pending.sortedWith { a, b -> ordering.compare(a.first, b.first) }
            for (index in 1 until sorted.size) {
                if (ordering.compare(sorted[index - 1].first, sorted[index].first) == 0) {
                    throw IllegalArgumentException("duplicate key: ${sorted[index].first}")
                }
            }
            return ImmutableSortedMap(sorted, ordering)
        }

        fun buildKeepingLast(): ImmutableSortedMap<K, V> =
            throw UnsupportedOperationException("ImmutableSortedMap.Builder does not implement buildKeepingLast")
    }

    companion object {
        private object NaturalComparator : Comparator<Comparable<Any?>> {
            override fun compare(a: Comparable<Any?>, b: Comparable<Any?>): Int = a.compareTo(b)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <K : Comparable<K>> natural(): Comparator<K> = NaturalComparator as Comparator<K>

        private val EMPTY_NATURAL = ImmutableSortedMap<Comparable<Any?>, Any>(emptyList(), NaturalComparator)

        @Suppress("UNCHECKED_CAST")
        private fun <K, V> emptyFor(ordering: Comparator<in K>): ImmutableSortedMap<K, V> =
            if (ordering === NaturalComparator) EMPTY_NATURAL as ImmutableSortedMap<K, V>
            else ImmutableSortedMap(emptyList(), ordering)

        @Suppress("UNCHECKED_CAST")
        fun <K : Comparable<K>, V> of(): ImmutableSortedMap<K, V> = EMPTY_NATURAL as ImmutableSortedMap<K, V>
        fun <K : Comparable<K>, V> of(k1: K, v1: V): ImmutableSortedMap<K, V> =
            naturalOrder<K, V>().put(k1, v1).build()
        fun <K : Comparable<K>, V> of(k1: K, v1: V, k2: K, v2: V): ImmutableSortedMap<K, V> =
            naturalOrder<K, V>().put(k1, v1).put(k2, v2).build()
        fun <K : Comparable<K>, V> of(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V): ImmutableSortedMap<K, V> =
            naturalOrder<K, V>().put(k1, v1).put(k2, v2).put(k3, v3).build()
        fun <K : Comparable<K>, V> of(
            k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V,
        ): ImmutableSortedMap<K, V> = naturalOrder<K, V>()
            .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).build()
        fun <K : Comparable<K>, V> of(
            k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V, k5: K, v5: V,
        ): ImmutableSortedMap<K, V> = naturalOrder<K, V>()
            .put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5).build()

        fun <K : Comparable<K>, V> copyOf(map: Map<out K, V>): ImmutableSortedMap<K, V> =
            copyOf(map, natural())

        fun <K, V> copyOf(map: Map<out K, V>, ordering: Comparator<in K>): ImmutableSortedMap<K, V> {
            Preconditions.checkNotNull(ordering)
            if (map is ImmutableSortedMap<*, *> && map.ordering == ordering) {
                @Suppress("UNCHECKED_CAST")
                return map as ImmutableSortedMap<K, V>
            }
            return Builder<K, V>(ordering).putAll(map).build()
        }

        fun <K : Comparable<K>, V> copyOf(entries: Iterable<Map.Entry<out K, V>>): ImmutableSortedMap<K, V> =
            naturalOrder<K, V>().putAll(entries).build()

        fun <K : Comparable<K>, V> copyOfSorted(map: Map<out K, V>): ImmutableSortedMap<K, V> =
            if (map is ImmutableSortedMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                map as ImmutableSortedMap<K, V>
            } else {
                copyOf(map)
            }

        fun <K : Comparable<K>, V> naturalOrder(): Builder<K, V> = Builder(natural())
        fun <K : Comparable<K>, V> reverseOrder(): Builder<K, V> = Builder(natural<K>().reversed())
        fun <K, V> orderedBy(ordering: Comparator<in K>): Builder<K, V> =
            Builder(Preconditions.checkNotNull(ordering))
        fun <K : Comparable<K>, V> builder(): Builder<K, V> = naturalOrder()

        private fun <T> nonNull(value: T, role: String): T =
            value ?: throw NullPointerException("null $role")
    }
}
