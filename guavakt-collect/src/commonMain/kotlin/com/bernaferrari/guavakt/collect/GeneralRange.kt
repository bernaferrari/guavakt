package com.bernaferrari.guavakt.collect

/**
 * Guava GeneralRange — range over a comparator (used by TreeRangeMap / TreeMultiset internals).
 */
internal class GeneralRange<T> private constructor(
    val comparator: Comparator<in T>,
    val hasLowerBound: Boolean,
    val lowerEndpoint: T?,
    val lowerBoundType: BoundType,
    val hasUpperBound: Boolean,
    val upperEndpoint: T?,
    val upperBoundType: BoundType,
) {
    fun contains(t: T): Boolean {
        if (tooLow(t) || tooHigh(t)) return false
        return true
    }

    fun tooLow(t: T): Boolean {
        if (!hasLowerBound) return false
        val cmp = comparator.compare(t, lowerEndpoint as T)
        return if (cmp < 0) true else if (cmp == 0) lowerBoundType == BoundType.OPEN else false
    }

    fun tooHigh(t: T): Boolean {
        if (!hasUpperBound) return false
        val cmp = comparator.compare(t, upperEndpoint as T)
        return if (cmp > 0) true else if (cmp == 0) upperBoundType == BoundType.OPEN else false
    }

    fun isEmpty(): Boolean {
        return (hasUpperBound && tooLow(upperEndpoint as T)) || (hasLowerBound && tooHigh(lowerEndpoint as T))
    }

    fun intersect(other: GeneralRange<T>): GeneralRange<T> {
        require(comparator == other.comparator)
        var hasLow = hasLowerBound
        var lowEnd = lowerEndpoint
        var lowType = lowerBoundType
        if (!hasLowerBound) {
            hasLow = other.hasLowerBound
            lowEnd = other.lowerEndpoint
            lowType = other.lowerBoundType
        } else if (other.hasLowerBound) {
            val cmp = comparator.compare(lowerEndpoint as T, other.lowerEndpoint as T)
            if (cmp < 0 || (cmp == 0 && other.lowerBoundType == BoundType.OPEN)) {
                lowEnd = other.lowerEndpoint
                lowType = other.lowerBoundType
            }
        }
        var hasUp = hasUpperBound
        var upEnd = upperEndpoint
        var upType = upperBoundType
        if (!hasUpperBound) {
            hasUp = other.hasUpperBound
            upEnd = other.upperEndpoint
            upType = other.upperBoundType
        } else if (other.hasUpperBound) {
            val cmp = comparator.compare(upperEndpoint as T, other.upperEndpoint as T)
            if (cmp > 0 || (cmp == 0 && other.upperBoundType == BoundType.OPEN)) {
                upEnd = other.upperEndpoint
                upType = other.upperBoundType
            }
        }
        if (hasLow && hasUp) {
            val cmp = comparator.compare(lowEnd as T, upEnd as T)
            if (cmp > 0 || (cmp == 0 && (lowType == BoundType.OPEN || upType == BoundType.OPEN))) {
                // empty range: low = up with open bounds
                lowEnd = upEnd
                lowType = BoundType.OPEN
                upType = BoundType.OPEN
            }
        }
        return GeneralRange(comparator, hasLow, lowEnd, lowType, hasUp, upEnd, upType)
    }

    companion object {
        fun <T> all(comparator: Comparator<in T>): GeneralRange<T> =
            GeneralRange(comparator, false, null, BoundType.OPEN, false, null, BoundType.OPEN)

        fun <T> downTo(comparator: Comparator<in T>, endpoint: T, boundType: BoundType): GeneralRange<T> =
            GeneralRange(comparator, true, endpoint, boundType, false, null, BoundType.OPEN)

        fun <T> upTo(comparator: Comparator<in T>, endpoint: T, boundType: BoundType): GeneralRange<T> =
            GeneralRange(comparator, false, null, BoundType.OPEN, true, endpoint, boundType)

        fun <T> range(
            comparator: Comparator<in T>,
            lower: T, lowerType: BoundType,
            upper: T, upperType: BoundType,
        ): GeneralRange<T> = GeneralRange(comparator, true, lower, lowerType, true, upper, upperType)

        fun <T : Comparable<T>> from(range: Range<T>): GeneralRange<T> {
            val hasLow = !range.hasLowerBound() // wait Guava: hasLowerBound
            // Use Range API if available
            return try {
                val low = if (range.hasLowerBound()) range.lowerEndpoint() else null
                val high = if (range.hasUpperBound()) range.upperEndpoint() else null
                GeneralRange(
                    naturalOrder(),
                    range.hasLowerBound(), low, if (range.hasLowerBound()) range.lowerBoundType() else BoundType.OPEN,
                    range.hasUpperBound(), high, if (range.hasUpperBound()) range.upperBoundType() else BoundType.OPEN,
                )
            } catch (_: Throwable) {
                all(naturalOrder())
            }
        }

        private fun <T : Comparable<T>> naturalOrder(): Comparator<T> = Comparator { a, b -> a.compareTo(b) }
    }
}
