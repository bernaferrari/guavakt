package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

@GwtCompatible
class Range<C : Comparable<C>> private constructor(
    private val lowerEndpoint: C?,
    private val lowerBoundType: BoundType?,
    private val upperEndpoint: C?,
    private val upperBoundType: BoundType?,
) {
    fun hasLowerBound(): Boolean = lowerEndpoint != null
    fun hasUpperBound(): Boolean = upperEndpoint != null
    fun lowerEndpoint(): C = Preconditions.checkNotNull(lowerEndpoint)
    fun upperEndpoint(): C = Preconditions.checkNotNull(upperEndpoint)
    fun lowerBoundType(): BoundType = Preconditions.checkNotNull(lowerBoundType)
    fun upperBoundType(): BoundType = Preconditions.checkNotNull(upperBoundType)

    fun contains(value: C): Boolean {
        Preconditions.checkNotNull(value)
        return lowerTest(value) && upperTest(value)
    }

    fun containsAll(values: Iterable<C>): Boolean = values.all { contains(it) }

    fun isEmpty(): Boolean {
        if (lowerEndpoint == null || upperEndpoint == null) return false
        // Guava's cuts compare endpoints by their natural ordering. `Comparable` is not required
        // to agree with `equals`, so two distinct values that compare equal still form an empty
        // half-open range (and an invalid open range).
        if (lowerEndpoint.compareTo(upperEndpoint) != 0) return false
        return lowerBoundType == BoundType.OPEN || upperBoundType == BoundType.OPEN
    }

    fun encloses(other: Range<C>): Boolean =
        compareLowers(this, other) <= 0 && compareUppers(this, other) >= 0

    /**
     * Guava: connected iff the ranges' intersection is non-empty **or** they touch at a bound
     * (including open/closed combinations where a single point is not in both but they abut).
     * Equivalent: this is not entirely to the left of other and other is not entirely to the left of this.
     */
    fun isConnected(other: Range<C>): Boolean =
        compareLowerToUpper(this, other) <= 0 && compareLowerToUpper(other, this) <= 0

    fun intersection(other: Range<C>): Range<C> {
        Preconditions.checkArgument(isConnected(other), "intersection called on disconnected ranges")
        val thisLowFirst = compareLowers(this, other) <= 0
        val thisUpLast = compareUppers(this, other) >= 0
        if (thisLowFirst && thisUpLast) return other
        if (!thisLowFirst && !thisUpLast) return this
        val low = if (thisLowFirst) other else this
        val up = if (thisUpLast) other else this
        return create(low.lowerEndpoint, low.lowerBoundType, up.upperEndpoint, up.upperBoundType)
    }

    /** Smallest range that encloses both this and [other]. */
    fun span(other: Range<C>): Range<C> {
        val thisLowFirst = compareLowers(this, other) <= 0
        val thisUpLast = compareUppers(this, other) >= 0
        if (thisLowFirst && thisUpLast) return this
        if (!thisLowFirst && !thisUpLast) return other
        val low = if (thisLowFirst) this else other
        val up = if (thisUpLast) this else other
        return create(low.lowerEndpoint, low.lowerBoundType, up.upperEndpoint, up.upperBoundType)
    }

    /** Gap between this and [other] if disconnected; throws if connected. */
    fun gap(other: Range<C>): Range<C> {
        Preconditions.checkArgument(!isConnected(other), "Ranges are connected")
        val thisFirst = compareLowers(this, other) < 0
        val lower = if (thisFirst) this else other
        val higher = if (thisFirst) other else this
        val lowerType = when (lower.upperBoundType) {
            BoundType.CLOSED -> BoundType.OPEN
            BoundType.OPEN -> BoundType.CLOSED
            null -> BoundType.OPEN
        }
        val upperType = when (higher.lowerBoundType) {
            BoundType.CLOSED -> BoundType.OPEN
            BoundType.OPEN -> BoundType.CLOSED
            null -> BoundType.OPEN
        }
        return create(lower.upperEndpoint, lowerType, higher.lowerEndpoint, upperType)
    }

    fun canonical(domain: DiscreteDomain<C>): Range<C> {
        var canonicalLower = lowerEndpoint
        var canonicalLowerType = lowerBoundType
        var canonicalUpper = upperEndpoint
        var canonicalUpperType = upperBoundType

        if (!hasLowerBound()) {
            try {
                canonicalLower = domain.minValue()
                canonicalLowerType = BoundType.CLOSED
            } catch (_: NoSuchElementException) {
                // Domains without a minimum retain their unbounded lower cut.
            }
        } else if (lowerBoundType == BoundType.OPEN) {
            val next = domain.next(lowerEndpoint())
            if (next == null) {
                // Guava represents this as an above-all cut. Range intentionally has no exposed
                // infinity cut, so use an equivalent, stable empty range instead.
                return closedOpen(lowerEndpoint(), lowerEndpoint())
            }
            canonicalLower = next
            canonicalLowerType = BoundType.CLOSED
        }

        if (hasUpperBound() && upperBoundType == BoundType.CLOSED) {
            val next = domain.next(upperEndpoint())
            if (next == null) {
                canonicalUpper = null
                canonicalUpperType = null
            } else {
                canonicalUpper = next
                canonicalUpperType = BoundType.OPEN
            }
        }

        return when {
            canonicalLower == null && canonicalUpper == null -> all()
            canonicalLower == null -> upTo(canonicalUpper!!, canonicalUpperType!!)
            canonicalUpper == null -> downTo(canonicalLower, canonicalLowerType!!)
            else -> range(canonicalLower, canonicalLowerType!!, canonicalUpper, canonicalUpperType!!)
        }
    }

    private fun lowerTest(value: C): Boolean {
        if (lowerEndpoint == null) return true
        val cmp = value.compareTo(lowerEndpoint)
        return if (lowerBoundType == BoundType.CLOSED) cmp >= 0 else cmp > 0
    }

    private fun upperTest(value: C): Boolean {
        if (upperEndpoint == null) return true
        val cmp = value.compareTo(upperEndpoint)
        return if (upperBoundType == BoundType.CLOSED) cmp <= 0 else cmp < 0
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Range<*>) return false
        return endpointEquals(lowerEndpoint, other.lowerEndpoint) && lowerBoundType == other.lowerBoundType &&
            endpointEquals(upperEndpoint, other.upperEndpoint) && upperBoundType == other.upperBoundType
    }

    override fun hashCode(): Int =
        lowerCutHashCode() * 31 + upperCutHashCode()

    /**
     * Matches Guava's `Cut.equals`: endpoints are equivalent when their natural ordering says
     * they are equal, not merely when `equals` does. A comparison may be ill-typed after type
     * erasure, in which case Guava returns false as well.
     */
    @Suppress("UNCHECKED_CAST")
    private fun endpointEquals(left: C?, right: Any?): Boolean = when {
        left == null || right == null -> left == null && right == null
        else -> try {
            left.compareTo(right as C) == 0
        } catch (_: ClassCastException) {
            false
        }
    }

    /**
     * Guava hashes finite lower/upper cuts as `endpoint`/`~endpoint` respectively according to
     * whether the endpoint is included. The unbounded-cut hashes are JVM object identities in
     * Guava; fixed sentinels keep Kotlin's value objects deterministic while preserving equality.
     */
    private fun lowerCutHashCode(): Int = when {
        lowerEndpoint == null -> Int.MIN_VALUE
        lowerBoundType == BoundType.CLOSED -> lowerEndpoint.hashCode()
        else -> lowerEndpoint.hashCode().inv()
    }

    private fun upperCutHashCode(): Int = when {
        upperEndpoint == null -> Int.MAX_VALUE
        upperBoundType == BoundType.OPEN -> upperEndpoint.hashCode()
        else -> upperEndpoint.hashCode().inv()
    }

    override fun toString(): String = buildString {
        append(if (lowerBoundType == BoundType.CLOSED) '[' else '(')
        append(lowerEndpoint ?: "-\u221e")
        append("..")
        append(upperEndpoint ?: "+\u221e")
        append(if (upperBoundType == BoundType.CLOSED) ']' else ')')
    }

    companion object {
        fun <C : Comparable<C>> open(lower: C, upper: C): Range<C> =
            create(lower, BoundType.OPEN, upper, BoundType.OPEN).also { Preconditions.checkArgument(!it.isEmpty()) }

        fun <C : Comparable<C>> closed(lower: C, upper: C): Range<C> =
            create(lower, BoundType.CLOSED, upper, BoundType.CLOSED)

        fun <C : Comparable<C>> closedOpen(lower: C, upper: C): Range<C> =
            create(lower, BoundType.CLOSED, upper, BoundType.OPEN)

        fun <C : Comparable<C>> openClosed(lower: C, upper: C): Range<C> =
            create(lower, BoundType.OPEN, upper, BoundType.CLOSED)

        fun <C : Comparable<C>> greaterThan(endpoint: C): Range<C> =
            create(endpoint, BoundType.OPEN, null, null)

        fun <C : Comparable<C>> atLeast(endpoint: C): Range<C> =
            create(endpoint, BoundType.CLOSED, null, null)

        fun <C : Comparable<C>> lessThan(endpoint: C): Range<C> =
            create(null, null, endpoint, BoundType.OPEN)

        fun <C : Comparable<C>> atMost(endpoint: C): Range<C> =
            create(null, null, endpoint, BoundType.CLOSED)

        fun <C : Comparable<C>> all(): Range<C> = create(null, null, null, null)

        fun <C : Comparable<C>> singleton(value: C): Range<C> = closed(value, value)

        fun <C : Comparable<C>> range(
            lower: C, lowerType: BoundType, upper: C, upperType: BoundType,
        ): Range<C> = create(lower, lowerType, upper, upperType)

        fun <C : Comparable<C>> upTo(endpoint: C, boundType: BoundType): Range<C> =
            create(null, null, endpoint, boundType)

        fun <C : Comparable<C>> downTo(endpoint: C, boundType: BoundType): Range<C> =
            create(endpoint, boundType, null, null)

        private fun <C : Comparable<C>> create(
            lower: C?, lowerType: BoundType?, upper: C?, upperType: BoundType?,
        ): Range<C> {
            if (lower != null && upper != null) {
                val cmp = lower.compareTo(upper)
                // Guava rejects lower > upper. Equal endpoints with both OPEN is empty but valid.
                Preconditions.checkArgument(cmp <= 0) {
                    "Invalid range: lower endpoint ($lower) > upper endpoint ($upper)"
                }
            }
            return Range(lower, lowerType, upper, upperType)
        }

        private fun <C : Comparable<C>> compareLowers(a: Range<C>, b: Range<C>): Int {
            if (a.lowerEndpoint == null) return if (b.lowerEndpoint == null) 0 else -1
            if (b.lowerEndpoint == null) return 1
            val cmp = a.lowerEndpoint.compareTo(b.lowerEndpoint)
            if (cmp != 0) return cmp
            if (a.lowerBoundType == b.lowerBoundType) return 0
            return if (a.lowerBoundType == BoundType.OPEN) 1 else -1
        }

        private fun <C : Comparable<C>> compareUppers(a: Range<C>, b: Range<C>): Int {
            if (a.upperEndpoint == null) return if (b.upperEndpoint == null) 0 else 1
            if (b.upperEndpoint == null) return -1
            val cmp = a.upperEndpoint.compareTo(b.upperEndpoint)
            if (cmp != 0) return cmp
            if (a.upperBoundType == b.upperBoundType) return 0
            return if (a.upperBoundType == BoundType.OPEN) -1 else 1
        }

        /**
         * Compare [a]'s lower bound to [b]'s upper bound: negative if a.lower is strictly below b.upper
         * (ranges may still connect), positive if a starts after b ends (gap).
         */
        private fun <C : Comparable<C>> compareLowerToUpper(a: Range<C>, b: Range<C>): Int {
            if (a.lowerEndpoint == null) return -1 // a unbounded below cannot be after b
            if (b.upperEndpoint == null) return -1 // b unbounded above cannot end before a
            val cmp = a.lowerEndpoint.compareTo(b.upperEndpoint)
            if (cmp != 0) return cmp
            // Equal endpoints: disconnected only if both bounds are OPEN (empty contact).
            // Guava: [1,2] connected to [2,3]; (1,2) not connected to (2,3); [1,2) not connected to (2,3].
            // Connected when at least one side is CLOSED (shares the point) OR both open on the same point is NOT connected.
            return if (a.lowerBoundType == BoundType.OPEN && b.upperBoundType == BoundType.OPEN) 1 else -1
        }
    }
}
