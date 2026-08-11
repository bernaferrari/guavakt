package dev.guavakt.collect

import dev.guavakt.base.Preconditions

class RegularContiguousSet<C : Comparable<C>>(
    private val first: C,
    private val last: C,
    private val dom: DiscreteDomain<C>,
) : ContiguousSet<C>() {
    override fun first(): C = first
    override fun last(): C = last

    override val size: Int
        get() {
            val distance = dom.distance(first, last)
            return if (distance >= Int.MAX_VALUE - 1L) Int.MAX_VALUE else (distance + 1L).toInt()
        }

    override fun iterator(): MutableIterator<C> = object : MutableIterator<C> {
        private var next = first
        private var hasNextValue = true

        override fun hasNext(): Boolean = hasNextValue

        override fun next(): C {
            if (!hasNextValue) throw NoSuchElementException()
            val result = next
            if (result.compareTo(last) == 0) {
                hasNextValue = false
            } else {
                next = dom.next(result) ?: throw NoSuchElementException()
            }
            return result
        }

        override fun remove() = throw UnsupportedOperationException("ContiguousSet is immutable")
    }
    override fun range(): Range<C> = Range.closed(first, last)
    override fun range(lowerBoundType: BoundType, upperBoundType: BoundType): Range<C> {
        val lower = when (lowerBoundType) {
            BoundType.CLOSED -> Range.downTo(first, BoundType.CLOSED)
            BoundType.OPEN -> dom.previous(first)?.let { Range.downTo(it, BoundType.OPEN) } ?: Range.all()
        }
        val upper = when (upperBoundType) {
            BoundType.CLOSED -> Range.upTo(last, BoundType.CLOSED)
            BoundType.OPEN -> dom.next(last)?.let { Range.upTo(it, BoundType.OPEN) } ?: Range.all()
        }
        return lower.intersection(upper)
    }
    override fun domain(): DiscreteDomain<C> = dom
    override fun contains(element: C): Boolean = element >= first && element <= last
    override fun intersection(other: ContiguousSet<C>): ContiguousSet<C> {
        Preconditions.checkArgument(dom == other.domain(), "ContiguousSets must use the same DiscreteDomain")
        if (other.isEmpty()) return other
        val oFirst = other.first()
        val oLast = other.last()
        val nFirst = maxOf(first, oFirst)
        val nLast = minOf(last, oLast)
        return if (nFirst > nLast) EmptyContiguousSet(dom) else RegularContiguousSet(nFirst, nLast, dom)
    }
    override fun headSetImpl(toElement: C, inclusive: Boolean): ContiguousSet<C> {
        val end = if (inclusive) toElement else (dom.previous(toElement) ?: return EmptyContiguousSet(dom))
        return if (end < first) EmptyContiguousSet(dom) else RegularContiguousSet(first, minOf(last, end), dom)
    }
    override fun subSetImpl(
        fromElement: C,
        fromInclusive: Boolean,
        toElement: C,
        toInclusive: Boolean,
    ): ContiguousSet<C> =
        tailSetImpl(fromElement, fromInclusive).headSetImpl(toElement, toInclusive)

    override fun tailSetImpl(fromElement: C, inclusive: Boolean): ContiguousSet<C> {
        val start = if (inclusive) fromElement else (dom.next(fromElement) ?: return EmptyContiguousSet(dom))
        return if (start > last) EmptyContiguousSet(dom) else RegularContiguousSet(maxOf(first, start), last, dom)
    }
}
