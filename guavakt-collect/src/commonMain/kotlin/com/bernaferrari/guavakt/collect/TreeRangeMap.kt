package com.bernaferrari.guavakt.collect

/**
 * Guava TreeRangeMap — list of (range, value) entries; put splits/removes overlaps.
 */
class TreeRangeMap<K : Comparable<K>, V> private constructor(
    private val entries: MutableList<Pair<Range<K>, V>> = ArrayList(),
) : RangeMap<K, V> {
    private val emptySubRangeMap: RangeMap<K, V> by lazy { EmptySubRangeMap() }
    private var entriesVersion: Int = 0

    override fun get(key: K): V? = getEntry(key)?.value

    override fun getEntry(key: K): Map.Entry<Range<K>, V>? {
        for ((range, value) in entries) {
            if (range.contains(key)) {
                return object : Map.Entry<Range<K>, V> {
                    override val key: Range<K> get() = range
                    override val value: V get() = value
                }
            }
        }
        return null
    }

    override fun span(): Range<K> {
        check(entries.isNotEmpty()) { "empty range map" }
        var span = entries[0].first
        for (i in 1 until entries.size) {
            span = span.span(entries[i].first)
        }
        return span
    }

    override fun put(range: Range<K>, value: V) {
        if (range.isEmpty()) return
        remove(range)
        entries.add(range to value)
        sortEntries()
        entriesVersion++
    }

    override fun putCoalescing(range: Range<K>, value: V) {
        // Do not short-circuit empty ranges: Guava permits one to bridge two compatible entries.
        if (entries.isEmpty()) {
            put(range, value)
            return
        }
        put(coalescedRange(range, value), value)
    }

    /** Mirrors Guava's two-neighbour coalescing search without mutating this map. */
    private fun coalescedRange(range: Range<K>, value: V): Range<K> {
        var coalesced = range
        val lowerEntry = entries.lastOrNull { compareLowerBounds(it.first, range) < 0 }
        coalesced = coalesce(coalesced, value, lowerEntry)
        // This lookup deliberately uses the original upper cut, just as Guava does. It only
        // coalesces the immediate right candidate; unrelated already-adjacent equal entries keep
        // their existing segmentation.
        val upperEntry = entries.lastOrNull { compareLowerBoundToUpperBound(it.first, range) <= 0 }
        return coalesce(coalesced, value, upperEntry)
    }

    /**
     * Guava [RangeMap.merge]: on each key in [range], if unmapped put [value]; if mapped apply
     * [remappingFunction](old, value) and remove when it returns null. Non-overlap keys in [range]
     * receive [value] only — they do not inherit remapped results from other segments.
     */
    override fun merge(range: Range<K>, value: V, remappingFunction: (V, V) -> V?) {
        if (range.isEmpty()) return
        val oldEntries = entries.toList()
        val next = ArrayList<Pair<Range<K>, V>>()
        val covered = ArrayList<Range<K>>()
        for ((existing, oldVal) in oldEntries) {
            if (!existing.isConnected(range) || existing.intersection(range).isEmpty()) {
                next.add(existing to oldVal)
                continue
            }
            for (remnant in subtractRange(existing, range)) {
                next.add(remnant to oldVal)
            }
            val overlap = existing.intersection(range)
            if (!overlap.isEmpty()) {
                covered.add(overlap)
                val remapped = remappingFunction(oldVal, value)
                if (remapped != null) next.add(overlap to remapped)
            }
        }
        var unmapped: List<Range<K>> = listOf(range)
        for (c in covered) {
            unmapped = unmapped.flatMap { subtractRange(it, c) }
        }
        for (gap in unmapped) {
            if (!gap.isEmpty()) next.add(gap to value)
        }
        if (entries != next) {
            entries.clear()
            entries.addAll(next)
            sortEntries()
            entriesVersion++
        }
    }

    /** Parts of [from] not covered by [remove] when they overlap; otherwise [from] alone. */
    private fun subtractRange(from: Range<K>, remove: Range<K>): List<Range<K>> {
        if (!from.isConnected(remove) || from.intersection(remove).isEmpty()) return listOf(from)
        val out = ArrayList<Range<K>>(2)
        if (remove.hasLowerBound()) {
            val leftWindow = Range.upTo<K>(remove.lowerEndpoint(), remove.lowerBoundType().flipped())
            if (from.isConnected(leftWindow)) {
                val left = from.intersection(leftWindow)
                if (!left.isEmpty()) out.add(left)
            }
        }
        if (remove.hasUpperBound()) {
            val rightWindow = Range.downTo<K>(remove.upperEndpoint(), remove.upperBoundType().flipped())
            if (from.isConnected(rightWindow)) {
                val right = from.intersection(rightWindow)
                if (!right.isEmpty()) out.add(right)
            }
        }
        return out
    }

    private fun BoundType.flipped(): BoundType =
        if (this == BoundType.CLOSED) BoundType.OPEN else BoundType.CLOSED

    private fun coalesce(range: Range<K>, value: V, candidate: Pair<Range<K>, V>?): Range<K> =
        if (candidate != null && candidate.second == value && candidate.first.isConnected(range)) {
            range.span(candidate.first)
        } else {
            range
        }

    private fun sortEntries() {
        entries.sortWith { left, right -> compareLowerBounds(left.first, right.first) }
    }

    /** Orders ranges by Guava's lower cuts: closed starts sort before open starts at one endpoint. */
    private fun compareLowerBounds(left: Range<K>, right: Range<K>): Int {
        if (!left.hasLowerBound()) return if (!right.hasLowerBound()) 0 else -1
        if (!right.hasLowerBound()) return 1
        val endpointOrder = left.lowerEndpoint().compareTo(right.lowerEndpoint())
        if (endpointOrder != 0) return endpointOrder
        if (left.lowerBoundType() == right.lowerBoundType()) return 0
        return if (left.lowerBoundType() == BoundType.CLOSED) -1 else 1
    }

    /** Compares a range's lower cut with another range's upper cut. */
    private fun compareLowerBoundToUpperBound(lower: Range<K>, upper: Range<K>): Int {
        if (!lower.hasLowerBound() || !upper.hasUpperBound()) return -1
        val endpointOrder = lower.lowerEndpoint().compareTo(upper.upperEndpoint())
        if (endpointOrder != 0) return endpointOrder
        return if (lower.lowerBoundType() == BoundType.OPEN && upper.upperBoundType() == BoundType.OPEN) 1 else -1
    }

    override fun putAll(rangeMap: RangeMap<K, V>) {
        for ((r, v) in rangeMap.asMapOfRanges()) put(r, v)
    }

    override fun clear() {
        if (entries.isNotEmpty()) {
            entries.clear()
            entriesVersion++
        }
    }

    override fun remove(range: Range<K>) {
        if (range.isEmpty()) return
        val next = ArrayList<Pair<Range<K>, V>>()
        for ((existing, value) in entries) {
            if (!existing.isConnected(range) || existing.intersection(range).isEmpty()) {
                next.add(existing to value)
                continue
            }
            for (remnant in subtractRange(existing, range)) {
                next.add(remnant to value)
            }
        }
        if (entries != next) {
            entries.clear()
            entries.addAll(next)
            entriesVersion++
        }
    }

    /**
     * Live map view in ascending cut order.
     *
     * Adding or replacing mappings through this view is unsupported; use [put]. Removing an
     * exposed mapping, entry-iterator removal, and [MutableMap.clear] write through.
     */
    override fun asMapOfRanges(): MutableMap<Range<K>, V> =
        LiveRangeMapView({ entries.toList() }, ::remove, ::clear)

    override fun asDescendingMapOfRanges(): MutableMap<Range<K>, V> =
        LiveRangeMapView({ entries.asReversed().toList() }, ::remove, ::clear)

    override fun subRangeMap(range: Range<K>): RangeMap<K, V> {
        return if (range == Range.all<K>()) this else SubRangeMapView(range)
    }

    /** A live, clipped sub-range view backed by this map. */
    private inner class SubRangeMapView(
        private val restriction: Range<K>,
    ) : RangeMap<K, V> {
        private fun visibleEntries(): List<Pair<Range<K>, V>> {
            val result = ArrayList<Pair<Range<K>, V>>()
            for ((range, value) in entries) {
                if (range.isConnected(restriction)) {
                    val clipped = range.intersection(restriction)
                    if (!clipped.isEmpty()) result.add(clipped to value)
                }
            }
            return result
        }

        override fun get(key: K): V? = if (restriction.contains(key)) this@TreeRangeMap[key] else null

        override fun getEntry(key: K): Map.Entry<Range<K>, V>? {
            if (!restriction.contains(key)) return null
            val entry = this@TreeRangeMap.getEntry(key) ?: return null
            return immutableEntry(entry.key.intersection(restriction), entry.value)
        }

        override fun span(): Range<K> {
            val visible = visibleEntries()
            if (visible.isEmpty()) throw NoSuchElementException()
            return visible.first().first.span(visible.last().first)
        }

        override fun put(range: Range<K>, value: V) {
            require(restriction.encloses(range)) {
                "Cannot put range $range into a subRangeMap($restriction)"
            }
            this@TreeRangeMap.put(range, value)
        }

        override fun putCoalescing(range: Range<K>, value: V) {
            if (entries.isEmpty() || !restriction.encloses(range)) {
                put(range, value)
                return
            }
            put(this@TreeRangeMap.coalescedRange(range, value).intersection(restriction), value)
        }

        override fun merge(range: Range<K>, value: V, remappingFunction: (V, V) -> V?) {
            require(restriction.encloses(range)) {
                "Cannot merge range $range into a subRangeMap($restriction)"
            }
            this@TreeRangeMap.merge(range, value, remappingFunction)
        }

        override fun putAll(rangeMap: RangeMap<K, V>) {
            if (rangeMap.asMapOfRanges().isEmpty()) return
            require(restriction.encloses(rangeMap.span())) {
                "Cannot putAll rangeMap with span ${rangeMap.span()} into a subRangeMap($restriction)"
            }
            this@TreeRangeMap.putAll(rangeMap)
        }

        override fun clear() = this@TreeRangeMap.remove(restriction)

        override fun remove(range: Range<K>) {
            if (range.isConnected(restriction)) this@TreeRangeMap.remove(range.intersection(restriction))
        }

        override fun asMapOfRanges(): MutableMap<Range<K>, V> =
            LiveRangeMapView(::visibleEntries, ::remove, ::clear)

        override fun asDescendingMapOfRanges(): MutableMap<Range<K>, V> =
            LiveRangeMapView({ visibleEntries().asReversed() }, ::remove, ::clear)

        override fun subRangeMap(range: Range<K>): RangeMap<K, V> =
            if (!range.isConnected(restriction)) emptySubRangeMap
            else this@TreeRangeMap.subRangeMap(range.intersection(restriction))

        override fun equals(other: Any?): Boolean =
            other is RangeMap<*, *> && asMapOfRanges() == other.asMapOfRanges()

        override fun hashCode(): Int = asMapOfRanges().hashCode()
        override fun toString(): String = asMapOfRanges().toString()
    }

    /** The Guava singleton-style result of asking a sub-range view for a disconnected view. */
    private inner class EmptySubRangeMap : RangeMap<K, V> {
        override fun get(key: K): V? = null
        override fun getEntry(key: K): Map.Entry<Range<K>, V>? = null
        override fun span(): Range<K> = throw NoSuchElementException()

        override fun put(range: Range<K>, value: V): Nothing =
            throw IllegalArgumentException("Cannot insert range $range into an empty subRangeMap")

        override fun putCoalescing(range: Range<K>, value: V): Nothing = put(range, value)

        override fun merge(range: Range<K>, value: V, remappingFunction: (V, V) -> V?): Nothing =
            throw IllegalArgumentException("Cannot merge range $range into an empty subRangeMap")

        override fun putAll(rangeMap: RangeMap<K, V>) {
            require(rangeMap.asMapOfRanges().isEmpty()) {
                "Cannot putAll(nonEmptyRangeMap) into an empty subRangeMap"
            }
        }

        override fun clear() = Unit
        override fun remove(range: Range<K>) = Unit
        override fun asMapOfRanges(): Map<Range<K>, V> = emptyMap()
        override fun asDescendingMapOfRanges(): Map<Range<K>, V> = emptyMap()
        override fun subRangeMap(range: Range<K>): RangeMap<K, V> = this
    }

    /**
     * A live removal-capable map view over a root or clipped range-map ordering.
     *
     * Guava's map-of-ranges views reject new mappings, but support removal through the map, entry,
     * key, and value collections. Kotlin callers see [MutableMap] only on the concrete mutable
     * `TreeRangeMap`; the [RangeMap] interface remains read-only for immutable implementations.
     */
    private inner class LiveRangeMapView(
        private val currentEntries: () -> List<Pair<Range<K>, V>>,
        private val removeRange: (Range<K>) -> Unit,
        private val clearRanges: () -> Unit,
    ) : AbstractMutableMap<Range<K>, V>() {
        private val entryView: MutableSet<MutableMap.MutableEntry<Range<K>, V>> by lazy {
            LiveEntrySet()
        }

        override val entries: MutableSet<MutableMap.MutableEntry<Range<K>, V>> get() = entryView

        override fun containsKey(key: Range<K>): Boolean = currentEntries().any { it.first == key }

        override fun get(key: Range<K>): V? = currentEntries().firstOrNull { it.first == key }?.second

        override fun put(key: Range<K>, value: V): Nothing =
            throw UnsupportedOperationException("Add ranges through TreeRangeMap.put")

        override fun remove(key: Range<K>): V? {
            val entry = currentEntries().firstOrNull { it.first == key } ?: return null
            removeRange(entry.first)
            return entry.second
        }

        override fun clear() = clearRanges()

        private fun immutableEntry(range: Range<K>, value: V): MutableMap.MutableEntry<Range<K>, V> =
            object : MutableMap.MutableEntry<Range<K>, V> {
                override val key: Range<K> = range
                override val value: V = value
                override fun setValue(newValue: V): Nothing =
                    throw UnsupportedOperationException("Range-map entries are immutable")

                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value

                override fun hashCode(): Int = key.hashCode() xor (value?.hashCode() ?: 0)
            }

        private inner class LiveEntrySet : AbstractMutableSet<MutableMap.MutableEntry<Range<K>, V>>() {
            override val size: Int get() = currentEntries().size

            override fun contains(element: MutableMap.MutableEntry<Range<K>, V>): Boolean =
                currentEntries().any { it.first == element.key && it.second == element.value }

            override fun iterator(): MutableIterator<MutableMap.MutableEntry<Range<K>, V>> {
                val snapshot = currentEntries().toList()
                var index = 0
                var last: Pair<Range<K>, V>? = null
                var canRemove = false
                var expectedVersion = entriesVersion

                fun checkForConcurrentModification() {
                    if (expectedVersion != entriesVersion) throw ConcurrentModificationException()
                }

                return object : MutableIterator<MutableMap.MutableEntry<Range<K>, V>> {
                    override fun hasNext(): Boolean {
                        checkForConcurrentModification()
                        return index < snapshot.size
                    }

                    override fun next(): MutableMap.MutableEntry<Range<K>, V> {
                        checkForConcurrentModification()
                        if (index >= snapshot.size) throw NoSuchElementException()
                        return snapshot[index++].also {
                            last = it
                            canRemove = true
                        }.let { immutableEntry(it.first, it.second) }
                    }

                    override fun remove() {
                        checkForConcurrentModification()
                        check(canRemove) { "next() must be called before remove()" }
                        val entry = checkNotNull(last)
                        if (entry !in currentEntries()) throw ConcurrentModificationException()
                        removeRange(entry.first)
                        expectedVersion = entriesVersion
                        canRemove = false
                    }
                }
            }

            override fun add(element: MutableMap.MutableEntry<Range<K>, V>): Nothing =
                throw UnsupportedOperationException("Add ranges through TreeRangeMap.put")

            override fun remove(element: MutableMap.MutableEntry<Range<K>, V>): Boolean {
                val entry = currentEntries().firstOrNull {
                    it.first == element.key && it.second == element.value
                } ?: return false
                removeRange(entry.first)
                return true
            }

            override fun clear() = clearRanges()
        }
    }

    private fun immutableEntry(range: Range<K>, value: V): Map.Entry<Range<K>, V> =
        object : Map.Entry<Range<K>, V> {
            override val key: Range<K> = range
            override val value: V = value
        }

    override fun equals(other: Any?): Boolean =
        other is RangeMap<*, *> && asMapOfRanges() == other.asMapOfRanges()

    override fun hashCode(): Int = asMapOfRanges().hashCode()
    override fun toString(): String = asMapOfRanges().toString()

    companion object {
        fun <K : Comparable<K>, V> create(): TreeRangeMap<K, V> = TreeRangeMap()
    }
}
