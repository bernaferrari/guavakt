package dev.guavakt.collect

/**
 * Guava TreeRangeSet — coalesced ranges in comparator order (portable sorted list).
 * Supports partial [remove] (subtract) and [complement] over the unbounded domain.
 */
class TreeRangeSet<C : Comparable<C>> private constructor(
    private val ranges: MutableList<Range<C>> = ArrayList(),
) : RangeSet<C> {
    private var complementView: RangeSet<C>? = null
    private var rangesVersion: Int = 0
    private val ascendingRangesView: MutableSet<Range<C>> by lazy {
        RangeCollectionView({ ranges }, ::remove, ::clear)
    }
    private val descendingRangesView: MutableSet<Range<C>> by lazy {
        RangeCollectionView({ ranges.asReversed() }, ::remove, ::clear)
    }

    override fun isEmpty(): Boolean = ranges.isEmpty()
    /**
     * A live, removal-capable collection view in ascending cut order.
     *
     * New ranges cannot be added through this collection: use [add] so connected ranges are
     * coalesced. Removing a currently exposed range, iterator removal, and [MutableSet.clear]
     * write through to this range set.
     */
    override fun asRanges(): MutableSet<Range<C>> = ascendingRangesView
    override fun asDescendingSetOfRanges(): MutableSet<Range<C>> = descendingRangesView
    override fun contains(value: C): Boolean = ranges.any { it.contains(value) }
    override fun rangeContaining(value: C): Range<C>? = ranges.firstOrNull { it.contains(value) }

    override fun encloses(otherRange: Range<C>): Boolean =
        ranges.any { it.encloses(otherRange) }

    override fun add(range: Range<C>) {
        if (range.isEmpty()) return
        val next = ArrayList<Range<C>>()
        var cur = range
        for (existing in ranges) {
            if (cur.isConnected(existing)) cur = cur.span(existing) else next.add(existing)
        }
        next.add(cur)
        val coalesced = coalesceAll(next)
        if (coalesced != ranges) {
            ranges.clear()
            ranges.addAll(coalesced)
            rangesVersion++
        }
    }

    override fun remove(range: Range<C>) {
        if (range.isEmpty() || ranges.isEmpty()) return
        val next = ArrayList<Range<C>>()
        for (existing in ranges) {
            if (!existing.isConnected(range) || existing.intersection(range).isEmpty()) {
                next.add(existing)
                continue
            }
            next.addAll(subtractRange(existing, range))
        }
        val coalesced = coalesceAll(next)
        if (coalesced != ranges) {
            ranges.clear()
            ranges.addAll(coalesced)
            rangesVersion++
        }
    }

    override fun clear() {
        if (ranges.isNotEmpty()) {
            ranges.clear()
            rangesVersion++
        }
    }

    private fun subtractRange(from: Range<C>, remove: Range<C>): List<Range<C>> {
        val out = ArrayList<Range<C>>(2)
        if (remove.hasLowerBound()) {
            val leftWindow = Range.upTo<C>(remove.lowerEndpoint(), remove.lowerBoundType().flipped())
            if (from.isConnected(leftWindow)) {
                val left = from.intersection(leftWindow)
                if (!left.isEmpty()) out.add(left)
            }
        }
        if (remove.hasUpperBound()) {
            val rightWindow = Range.downTo<C>(remove.upperEndpoint(), remove.upperBoundType().flipped())
            if (from.isConnected(rightWindow)) {
                val right = from.intersection(rightWindow)
                if (!right.isEmpty()) out.add(right)
            }
        }
        return out
    }

    private fun BoundType.flipped(): BoundType =
        if (this == BoundType.CLOSED) BoundType.OPEN else BoundType.CLOSED

    /**
     * A cached, live view over one ordering of a range set's exposed ranges.
     *
     * Guava's `asRanges()` is backed by a map-values view: it permits removals but not additions.
     * Kotlin exposes that distinction directly with [MutableSet], while [RangeSet] keeps the
     * portable interface read-only so immutable implementations never advertise mutation.
     */
    private inner class RangeCollectionView(
        private val currentRanges: () -> List<Range<C>>,
        private val removeRange: (Range<C>) -> Unit,
        private val clearRanges: () -> Unit,
    ) : AbstractMutableSet<Range<C>>() {
        override val size: Int get() = currentRanges().size

        override fun contains(element: Range<C>): Boolean = element in currentRanges()

        override fun iterator(): MutableIterator<Range<C>> {
            val snapshot = currentRanges().toList()
            var index = 0
            var last: Range<C>? = null
            var canRemove = false
            var expectedVersion = rangesVersion

            fun checkForConcurrentModification() {
                if (expectedVersion != rangesVersion) throw ConcurrentModificationException()
            }

            return object : MutableIterator<Range<C>> {
                override fun hasNext(): Boolean {
                    checkForConcurrentModification()
                    return index < snapshot.size
                }

                override fun next(): Range<C> {
                    checkForConcurrentModification()
                    if (index >= snapshot.size) throw NoSuchElementException()
                    return snapshot[index++].also {
                        last = it
                        canRemove = true
                    }
                }

                override fun remove() {
                    checkForConcurrentModification()
                    check(canRemove) { "next() must be called before remove()" }
                    val range = checkNotNull(last)
                    if (range !in currentRanges()) throw ConcurrentModificationException()
                    removeRange(range)
                    expectedVersion = rangesVersion
                    canRemove = false
                }
            }
        }

        override fun add(element: Range<C>): Boolean =
            throw UnsupportedOperationException("Add ranges through TreeRangeSet.add")

        override fun remove(element: Range<C>): Boolean {
            if (element !in currentRanges()) return false
            removeRange(element)
            return true
        }

        override fun clear() = clearRanges()
    }

    /** A cached live view for derived range sets, whose generated ranges cannot be removed safely. */
    private inner class ReadOnlyRangeCollectionView(
        private val currentRanges: () -> List<Range<C>>,
    ) : AbstractSet<Range<C>>() {
        override val size: Int get() = currentRanges().size
        override fun contains(element: Range<C>): Boolean = element in currentRanges()
        override fun iterator(): Iterator<Range<C>> {
            val snapshot = currentRanges().toList()
            var index = 0
            val expectedVersion = rangesVersion

            fun checkForConcurrentModification() {
                if (expectedVersion != rangesVersion) throw ConcurrentModificationException()
            }

            return object : Iterator<Range<C>> {
                override fun hasNext(): Boolean {
                    checkForConcurrentModification()
                    return index < snapshot.size
                }

                override fun next(): Range<C> {
                    checkForConcurrentModification()
                    if (index >= snapshot.size) throw NoSuchElementException()
                    return snapshot[index++]
                }
            }
        }
    }

    /**
     * A live complement view. Adding to the view removes from this range set and vice versa.
     */
    override fun complement(): RangeSet<C> = complementView ?: ComplementView(this, Range.all()).also {
        complementView = it
    }

    /** A live, clipped view backed by this range set. */
    override fun subRangeSet(view: Range<C>): RangeSet<C> =
        if (view == Range.all<C>()) this else SubRangeSetView(this, view)

    /** Builds a detached range-set representation of [source]'s complement inside [universe]. */
    private fun complementSnapshot(source: RangeSet<C>, universe: Range<C>): TreeRangeSet<C> {
        val out = create<C>()
        out.add(universe)
        out.removeAll(source.asRanges())
        return out
    }

    private inner class ComplementView(
        private val source: RangeSet<C>,
        private val universe: Range<C>,
    ) : RangeSet<C> {
        private fun snapshot(): TreeRangeSet<C> = complementSnapshot(source, universe)
        private fun currentRanges(): List<Range<C>> = snapshot().asRanges().toList()
        private val ascendingRangesView: Set<Range<C>> by lazy {
            ReadOnlyRangeCollectionView(::currentRanges)
        }
        private val descendingRangesView: Set<Range<C>> by lazy {
            ReadOnlyRangeCollectionView { currentRanges().asReversed() }
        }

        override fun contains(value: C): Boolean = universe.contains(value) && !source.contains(value)
        override fun rangeContaining(value: C): Range<C>? = snapshot().rangeContaining(value)
        override fun encloses(otherRange: Range<C>): Boolean = snapshot().encloses(otherRange)
        override fun asRanges(): Set<Range<C>> = ascendingRangesView
        override fun asDescendingSetOfRanges(): Set<Range<C>> = descendingRangesView
        override fun isEmpty(): Boolean = snapshot().isEmpty()

        override fun add(range: Range<C>) {
            if (range.isConnected(universe)) source.remove(range.intersection(universe))
        }

        override fun remove(range: Range<C>) {
            if (range.isConnected(universe)) source.add(range.intersection(universe))
        }

        override fun clear() = source.add(universe)
        override fun complement(): RangeSet<C> = source
        override fun subRangeSet(view: Range<C>): RangeSet<C> =
            if (view.encloses(universe)) this
            else if (view.isConnected(universe)) SubRangeSetView(this, universe.intersection(view))
            else ImmutableRangeSet.of()
    }

    private inner class SubRangeSetView(
        private val source: RangeSet<C>,
        private val restriction: Range<C>,
    ) : RangeSet<C> {
        private fun clippedRanges(): LinkedHashSet<Range<C>> {
            val result = LinkedHashSet<Range<C>>()
            for (range in source.asRanges()) {
                if (range.isConnected(restriction)) {
                    val clipped = range.intersection(restriction)
                    if (!clipped.isEmpty()) result.add(clipped)
                }
            }
            return result
        }
        private val ascendingRangesView: Set<Range<C>> by lazy {
            ReadOnlyRangeCollectionView { clippedRanges().toList() }
        }
        private val descendingRangesView: Set<Range<C>> by lazy {
            ReadOnlyRangeCollectionView { clippedRanges().toList().asReversed() }
        }

        override fun contains(value: C): Boolean = restriction.contains(value) && source.contains(value)
        override fun rangeContaining(value: C): Range<C>? {
            if (!restriction.contains(value)) return null
            val containing = source.rangeContaining(value) ?: return null
            return containing.intersection(restriction)
        }
        override fun encloses(otherRange: Range<C>): Boolean =
            !restriction.isEmpty() && restriction.encloses(otherRange) && source.encloses(otherRange)
        override fun asRanges(): Set<Range<C>> = ascendingRangesView
        override fun asDescendingSetOfRanges(): Set<Range<C>> = descendingRangesView
        override fun isEmpty(): Boolean = clippedRanges().isEmpty()

        override fun add(range: Range<C>) {
            require(restriction.encloses(range)) {
                "Cannot add range $range to subRangeSet($restriction)"
            }
            source.add(range)
        }

        override fun remove(range: Range<C>) {
            if (range.isConnected(restriction)) source.remove(range.intersection(restriction))
        }

        override fun clear() = source.remove(restriction)
        // Guava's sub-range view is itself a RangeSet, so its complement is over the full
        // unbounded universe rather than only the restriction.
        override fun complement(): RangeSet<C> = ComplementView(this, Range.all())
        override fun subRangeSet(view: Range<C>): RangeSet<C> = when {
            view.encloses(restriction) -> this
            view.isConnected(restriction) -> SubRangeSetView(source, restriction.intersection(view))
            else -> ImmutableRangeSet.of()
        }
    }

    private fun coalesceAll(input: List<Range<C>>): List<Range<C>> {
        if (input.isEmpty()) return emptyList()
        val sorted = input.sortedWith(::compareLowerBounds)
        val out = ArrayList<Range<C>>()
        var cur = sorted[0]
        for (i in 1 until sorted.size) {
            val n = sorted[i]
            if (cur.isConnected(n)) cur = cur.span(n) else { out.add(cur); cur = n }
        }
        out.add(cur)
        return out
    }

    /** Orders ranges by Guava's lower cuts: closed starts sort before open starts at one endpoint. */
    private fun compareLowerBounds(left: Range<C>, right: Range<C>): Int {
        if (!left.hasLowerBound()) return if (!right.hasLowerBound()) 0 else -1
        if (!right.hasLowerBound()) return 1
        val endpointOrder = left.lowerEndpoint().compareTo(right.lowerEndpoint())
        if (endpointOrder != 0) return endpointOrder
        if (left.lowerBoundType() == right.lowerBoundType()) return 0
        return if (left.lowerBoundType() == BoundType.CLOSED) -1 else 1
    }

    companion object {
        fun <C : Comparable<C>> create(): TreeRangeSet<C> = TreeRangeSet()
        fun <C : Comparable<C>> create(ranges: Iterable<Range<C>>): TreeRangeSet<C> =
            create<C>().also { for (r in ranges) it.add(r) }
    }
}
