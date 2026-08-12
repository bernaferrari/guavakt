package com.bernaferrari.guavakt.collect

/** Immutable, lower-bound-sorted snapshot of disjoint range mappings. */
class ImmutableRangeMap<K : Comparable<K>, V> private constructor(
    private val ranges: List<Range<K>>,
    private val values: List<V>,
) : RangeMap<K, V> {
    private val ascendingMap: ImmutableMap<Range<K>, V> = immutableMap(ranges.indices)
    private val descendingMap: ImmutableMap<Range<K>, V> = immutableMap(ranges.indices.reversed())

    override fun get(key: K): V? = getEntry(key)?.value

    override fun getEntry(key: K): Map.Entry<Range<K>, V>? {
        nonNull(key, "key")
        for (index in ranges.indices) {
            if (ranges[index].contains(key)) return Maps.immutableEntry(ranges[index], values[index])
        }
        return null
    }

    override fun span(): Range<K> {
        if (ranges.isEmpty()) throw NoSuchElementException()
        return ranges.first().span(ranges.last())
    }

    override fun put(range: Range<K>, value: V): Unit = throw UnsupportedOperationException("ImmutableRangeMap")
    override fun putCoalescing(range: Range<K>, value: V): Unit = throw UnsupportedOperationException("ImmutableRangeMap")
    override fun merge(range: Range<K>, value: V, remappingFunction: (V, V) -> V?): Unit =
        throw UnsupportedOperationException("ImmutableRangeMap")
    override fun putAll(rangeMap: RangeMap<K, V>): Unit = throw UnsupportedOperationException("ImmutableRangeMap")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableRangeMap")
    override fun remove(range: Range<K>): Unit = throw UnsupportedOperationException("ImmutableRangeMap")

    override fun asMapOfRanges(): ImmutableMap<Range<K>, V> = ascendingMap
    override fun asDescendingMapOfRanges(): ImmutableMap<Range<K>, V> = descendingMap

    override fun subRangeMap(view: Range<K>): ImmutableRangeMap<K, V> {
        val checked = nonNull(view, "range")
        if (checked.isEmpty() || ranges.isEmpty()) return of()
        if (checked.encloses(span())) return this
        val subRanges = ArrayList<Range<K>>()
        val subValues = ArrayList<V>()
        for (index in ranges.indices) {
            val current = ranges[index]
            if (current.isConnected(checked)) {
                val intersection = current.intersection(checked)
                if (!intersection.isEmpty()) {
                    subRanges.add(intersection)
                    subValues.add(values[index])
                }
            }
        }
        return fromSorted(subRanges, subValues)
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is RangeMap<*, *> && asMapOfRanges() == other.asMapOfRanges())
    override fun hashCode(): Int = asMapOfRanges().hashCode()
    override fun toString(): String = asMapOfRanges().toString()

    private fun immutableMap(indices: Iterable<Int>): ImmutableMap<Range<K>, V> {
        val builder = ImmutableMap.builder<Range<K>, V>()
        indices.forEach { index -> builder.put(ranges[index], values[index]) }
        return builder.buildOrThrow()
    }

    class Builder<K : Comparable<K>, V> {
        private val entries = ArrayList<Pair<Range<K>, V>>()

        fun put(range: Range<K>, value: V): Builder<K, V> = apply {
            val checkedRange = nonNull(range, "range")
            val checkedValue = nonNull(value, "value")
            require(!checkedRange.isEmpty()) { "Range must not be empty, but was $checkedRange" }
            entries.add(checkedRange to checkedValue)
        }

        fun putAll(rangeMap: RangeMap<K, out V>): Builder<K, V> = apply {
            rangeMap.asMapOfRanges().forEach { (range, value) -> put(range, value) }
        }

        fun build(): ImmutableRangeMap<K, V> {
            if (entries.isEmpty()) return of()
            val sorted = entries.sortedWith { first, second -> rangeLexComparator<K>().compare(first.first, second.first) }
            for (index in 1 until sorted.size) {
                val previous = sorted[index - 1].first
                val current = sorted[index].first
                if (previous.isConnected(current) && !previous.intersection(current).isEmpty()) {
                    throw IllegalArgumentException("Overlapping ranges: range $previous overlaps with entry $current")
                }
            }
            return fromSorted(sorted.map { it.first }, sorted.map { it.second })
        }
    }

    companion object {
        private val EMPTY = ImmutableRangeMap<Int, Any>(emptyList(), emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <K : Comparable<K>, V> of(): ImmutableRangeMap<K, V> = EMPTY as ImmutableRangeMap<K, V>

        fun <K : Comparable<K>, V> of(range: Range<K>, value: V): ImmutableRangeMap<K, V> =
            ImmutableRangeMap(listOf(nonNull(range, "range")), listOf(nonNull(value, "value")))

        fun <K : Comparable<K>, V> copyOf(rangeMap: RangeMap<K, out V>): ImmutableRangeMap<K, V> {
            if (rangeMap is ImmutableRangeMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return rangeMap as ImmutableRangeMap<K, V>
            }
            val entries = rangeMap.asMapOfRanges().entries
            if (entries.isEmpty()) return of()
            val sorted = entries.map { nonNull(it.key, "range") to nonNull(it.value, "value") }
                .sortedWith { first, second -> rangeLexComparator<K>().compare(first.first, second.first) }
            return fromSorted(sorted.map { it.first }, sorted.map { it.second })
        }

        fun <K : Comparable<K>, V> builder(): Builder<K, V> = Builder()

        private fun <K : Comparable<K>, V> fromSorted(
            ranges: List<Range<K>>,
            values: List<V>,
        ): ImmutableRangeMap<K, V> = if (ranges.isEmpty()) of() else ImmutableRangeMap(ranges.toList(), values.toList())

        private fun <T> nonNull(value: T, role: String): T = value ?: throw NullPointerException("null $role")
    }
}
