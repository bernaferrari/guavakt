package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Preconditions

abstract class ContiguousSet<C : Comparable<C>> : AbstractSet<C>() {
    /**
     * Returns the least element without traversing the set.
     *
     * This deliberately shadows Kotlin's [Iterable.first] extension: a contiguous set may span
     * billions of values, and its endpoints are available in constant time.
     */
    abstract fun first(): C

    /**
     * Returns the greatest element without traversing the set.
     *
     * This deliberately shadows Kotlin's [Iterable.last] extension for the same reason as
     * [first].
     */
    abstract fun last(): C

    abstract fun range(): Range<C>
    abstract fun range(lowerBoundType: BoundType, upperBoundType: BoundType): Range<C>
    abstract fun domain(): DiscreteDomain<C>
    abstract fun intersection(other: ContiguousSet<C>): ContiguousSet<C>
    abstract fun headSetImpl(toElement: C, inclusive: Boolean): ContiguousSet<C>
    abstract fun subSetImpl(
        fromElement: C,
        fromInclusive: Boolean,
        toElement: C,
        toInclusive: Boolean,
    ): ContiguousSet<C>
    abstract fun tailSetImpl(fromElement: C, inclusive: Boolean): ContiguousSet<C>

    /** The values strictly below [toElement]. */
    fun headSet(toElement: C): ContiguousSet<C> = headSetImpl(toElement, inclusive = false)
    fun headSet(toElement: C, inclusive: Boolean): ContiguousSet<C> = headSetImpl(toElement, inclusive)

    /** The values from [fromElement] onward. */
    fun tailSet(fromElement: C): ContiguousSet<C> = tailSetImpl(fromElement, inclusive = true)
    fun tailSet(fromElement: C, inclusive: Boolean): ContiguousSet<C> = tailSetImpl(fromElement, inclusive)

    /** The values from [fromElement] (inclusive) through [toElement] (exclusive). */
    fun subSet(fromElement: C, toElement: C): ContiguousSet<C> =
        subSet(
            fromElement = fromElement,
            fromInclusive = true,
            toElement = toElement,
            toInclusive = false,
        )

    fun subSet(
        fromElement: C,
        fromInclusive: Boolean,
        toElement: C,
        toInclusive: Boolean,
    ): ContiguousSet<C> {
        Preconditions.checkArgument(fromElement <= toElement, "fromElement must be <= toElement")
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive)
    }

    override fun toString(): String = if (isEmpty()) "[]" else range().toString()

    companion object {
        fun create(range: Range<Int>): ContiguousSet<Int> = create(range, DiscreteDomain.integers())

        fun <C : Comparable<C>> create(range: Range<C>, domain: DiscreteDomain<C>): ContiguousSet<C> {
            var lo: C
            var hi: C
            try {
                lo = if (range.hasLowerBound()) range.lowerEndpoint() else domain.minValue()
                hi = if (range.hasUpperBound()) range.upperEndpoint() else domain.maxValue()
            } catch (exception: NoSuchElementException) {
                throw IllegalArgumentException(exception)
            }
            if (range.hasLowerBound() && range.lowerBoundType() == BoundType.OPEN) {
                lo = domain.next(lo) ?: return EmptyContiguousSet(domain)
            }
            if (range.hasUpperBound() && range.upperBoundType() == BoundType.OPEN) {
                hi = domain.previous(hi) ?: return EmptyContiguousSet(domain)
            }
            if (lo > hi) return EmptyContiguousSet(domain)
            return RegularContiguousSet(lo, hi, domain)
        }

        fun closed(lower: Int, upper: Int): ContiguousSet<Int> = create(Range.closed(lower, upper))

        fun closed(lower: Long, upper: Long): ContiguousSet<Long> =
            create(Range.closed(lower, upper), DiscreteDomain.longs())

        fun closedOpen(lower: Int, upper: Int): ContiguousSet<Int> = create(Range.closedOpen(lower, upper))

        fun closedOpen(lower: Long, upper: Long): ContiguousSet<Long> =
            create(Range.closedOpen(lower, upper), DiscreteDomain.longs())
    }
}
