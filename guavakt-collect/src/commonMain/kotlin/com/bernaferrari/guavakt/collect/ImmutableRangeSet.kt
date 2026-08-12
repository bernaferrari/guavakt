package com.bernaferrari.guavakt.collect

/** Immutable, sorted snapshot of disjoint ranges. */
class ImmutableRangeSet<C : Comparable<C>> private constructor(
    private val ranges: List<Range<C>>,
) : RangeSet<C> {
    private var complementView: ImmutableRangeSet<C>? = null
    private val ascendingRanges: ImmutableSet<Range<C>> = ImmutableSet.copyOf(ranges)
    private val descendingRanges: ImmutableSet<Range<C>> = ImmutableSet.copyOf(ranges.asReversed())

    override fun isEmpty(): Boolean = ranges.isEmpty()
    override fun asRanges(): ImmutableSet<Range<C>> = ascendingRanges
    override fun asDescendingSetOfRanges(): ImmutableSet<Range<C>> = descendingRanges
    override fun contains(value: C): Boolean = rangeContaining(value) != null
    override fun rangeContaining(value: C): Range<C>? = ranges.firstOrNull { it.contains(value) }
    override fun intersects(otherRange: Range<C>): Boolean = ranges.any { current ->
        current.isConnected(otherRange) && !current.intersection(otherRange).isEmpty()
    }
    override fun encloses(otherRange: Range<C>): Boolean = ranges.any { it.encloses(otherRange) }

    override fun span(): Range<C> {
        if (ranges.isEmpty()) throw NoSuchElementException()
        return ranges.first().span(ranges.last())
    }

    override fun subRangeSet(view: Range<C>): ImmutableRangeSet<C> {
        nonNull(view, "range")
        if (view.isEmpty() || ranges.isEmpty()) return of()
        if (view.encloses(span())) return this
        val intersections = ArrayList<Range<C>>()
        for (current in ranges) {
            if (current.isConnected(view)) {
                val intersection = current.intersection(view)
                if (!intersection.isEmpty()) intersections.add(intersection)
            }
        }
        return fromNormalized(intersections)
    }

    override fun complement(): ImmutableRangeSet<C> {
        complementView?.let { return it }
        val gaps = TreeRangeSet.create(ranges).complement().asRanges().toList()
        val complement = fromNormalized(gaps)
        complement.complementView = this
        complementView = complement
        return complement
    }

    fun union(other: RangeSet<C>): ImmutableRangeSet<C> =
        unionOf(asRanges() + other.asRanges())

    fun intersection(other: RangeSet<C>): ImmutableRangeSet<C> {
        val intersections = ArrayList<Range<C>>()
        for (left in ranges) {
            for (right in other.asRanges()) {
                if (left.isConnected(right)) {
                    val intersection = left.intersection(right)
                    if (!intersection.isEmpty()) intersections.add(intersection)
                }
            }
        }
        return unionOf(intersections)
    }

    fun difference(other: RangeSet<C>): ImmutableRangeSet<C> {
        val mutable = TreeRangeSet.create(ranges)
        mutable.removeAll(other)
        return copyOf(mutable)
    }

    /**
     * An immutable, lazily iterated view of the values in this range set for [domain].
     *
     * A view can be conceptually enormous: endpoint and membership operations are cheap, while
     * iteration, equality, and hashing may require visiting every value. Kotlin common has no
     * `NavigableSet`, so this is exposed as a read-only [Set] rather than a JVM-shaped sorted-set
     * type.
     *
     * @throws IllegalArgumentException when the ranges and domain together have no lower or upper
     * bound from which the view can be enumerated.
     */
    fun asSet(domain: DiscreteDomain<C>): Set<C> {
        if (ranges.isEmpty()) return ImmutableSet.of()
        val canonicalSpan = span().canonical(domain)
        if (!canonicalSpan.hasLowerBound()) {
            throw IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded below")
        }
        if (!canonicalSpan.hasUpperBound()) {
            try {
                domain.maxValue()
            } catch (exception: NoSuchElementException) {
                throw IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded above", exception)
            }
        }
        return AsSet(domain)
    }

    override fun add(range: Range<C>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun addAll(other: RangeSet<C>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun addAll(ranges: Iterable<Range<C>>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun remove(range: Range<C>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun removeAll(other: RangeSet<C>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun removeAll(ranges: Iterable<Range<C>>): Unit = throw UnsupportedOperationException("ImmutableRangeSet")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableRangeSet")

    override fun equals(other: Any?): Boolean =
        other === this || (other is RangeSet<*> && asRanges() == other.asRanges())
    override fun hashCode(): Int = asRanges().hashCode()
    override fun toString(): String = asRanges().toString()

    private inner class AsSet(
        private val domain: DiscreteDomain<C>,
    ) : AbstractMutableSet<C>() {
        private var computedSize: Int? = null

        override val size: Int
            get() = computedSize ?: run {
                var total = 0L
                for (range in ranges) {
                    total += ContiguousSet.create(range, domain).size.toLong()
                    if (total >= Int.MAX_VALUE) break
                }
                total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().also { computedSize = it }
            }

        override fun contains(element: C): Boolean = this@ImmutableRangeSet.contains(element)

        override fun iterator(): MutableIterator<C> = object : MutableIterator<C> {
            private var rangeIndex = 0
            private var elements: Iterator<C>? = null

            private fun advance(): Iterator<C>? {
                var current = elements
                while ((current == null || !current.hasNext()) && rangeIndex < ranges.size) {
                    current = ContiguousSet.create(ranges[rangeIndex++], domain).iterator()
                }
                elements = current
                return current
            }

            override fun hasNext(): Boolean = advance()?.hasNext() == true

            override fun next(): C = advance()?.next() ?: throw NoSuchElementException()

            override fun remove(): Unit = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        }

        override fun add(element: C): Boolean = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        override fun remove(element: C): Boolean = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        override fun addAll(elements: Collection<C>): Boolean = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        override fun removeAll(elements: Collection<C>): Boolean = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        override fun retainAll(elements: Collection<C>): Boolean = throw UnsupportedOperationException("ImmutableRangeSet.asSet")
        override fun clear(): Unit = throw UnsupportedOperationException("ImmutableRangeSet.asSet")

        override fun toString(): String = ranges.toString()
    }

    class Builder<C : Comparable<C>> {
        private val ranges = ArrayList<Range<C>>()

        fun add(range: Range<C>): Builder<C> = apply {
            val checked = nonNull(range, "range")
            require(!checked.isEmpty()) { "range must not be empty, but was $checked" }
            ranges.add(checked)
        }
        fun addAll(rangeSet: RangeSet<C>): Builder<C> = addAll(rangeSet.asRanges())
        fun addAll(ranges: Iterable<Range<C>>): Builder<C> = apply { ranges.forEach(::add) }
        fun build(): ImmutableRangeSet<C> = strictCopy(ranges)
    }

    companion object {
        private val EMPTY = ImmutableRangeSet<Int>(emptyList())
        private val ALL = ImmutableRangeSet(listOf(Range.all<Int>()))

        init {
            EMPTY.complementView = ALL
            ALL.complementView = EMPTY
        }

        @Suppress("UNCHECKED_CAST")
        fun <C : Comparable<C>> of(): ImmutableRangeSet<C> = EMPTY as ImmutableRangeSet<C>

        @Suppress("UNCHECKED_CAST")
        private fun <C : Comparable<C>> all(): ImmutableRangeSet<C> = ALL as ImmutableRangeSet<C>

        fun <C : Comparable<C>> of(range: Range<C>): ImmutableRangeSet<C> {
            val checked = nonNull(range, "range")
            return when {
                checked.isEmpty() -> of()
                checked == Range.all<C>() -> all()
                else -> ImmutableRangeSet(listOf(checked))
            }
        }

        fun <C : Comparable<C>> copyOf(rangeSet: RangeSet<C>): ImmutableRangeSet<C> {
            if (rangeSet is ImmutableRangeSet<*>) {
                @Suppress("UNCHECKED_CAST")
                return rangeSet as ImmutableRangeSet<C>
            }
            if (rangeSet.isEmpty()) return of()
            if (rangeSet.encloses(Range.all<C>())) return all()
            return fromNormalized(rangeSet.asRanges().toList())
        }

        fun <C : Comparable<C>> copyOf(ranges: Iterable<Range<C>>): ImmutableRangeSet<C> = strictCopy(ranges)

        fun <C : Comparable<C>> unionOf(ranges: Iterable<Range<C>>): ImmutableRangeSet<C> =
            copyOf(TreeRangeSet.create(ranges))

        fun <C : Comparable<C>> builder(): Builder<C> = Builder()

        private fun <C : Comparable<C>> strictCopy(source: Iterable<Range<C>>): ImmutableRangeSet<C> {
            val sorted = source.map { range ->
                val checked = nonNull(range, "range")
                require(!checked.isEmpty()) { "range must not be empty, but was $checked" }
                checked
            }.sortedWith(rangeLexComparator())
            if (sorted.isEmpty()) return of()

            val normalized = ArrayList<Range<C>>(sorted.size)
            var current = sorted.first()
            for (index in 1 until sorted.size) {
                val next = sorted[index]
                if (current.isConnected(next)) {
                    require(current.intersection(next).isEmpty()) {
                        "Overlapping ranges not permitted but found $current overlapping $next"
                    }
                    current = current.span(next)
                } else {
                    normalized.add(current)
                    current = next
                }
            }
            normalized.add(current)
            return fromNormalized(normalized)
        }

        private fun <C : Comparable<C>> fromNormalized(source: List<Range<C>>): ImmutableRangeSet<C> = when {
            source.isEmpty() -> of()
            source.size == 1 && source[0] == Range.all<C>() -> all()
            else -> ImmutableRangeSet(source.toList())
        }

        private fun <T> nonNull(value: T, role: String): T = value ?: throw NullPointerException("null $role")
    }
}
