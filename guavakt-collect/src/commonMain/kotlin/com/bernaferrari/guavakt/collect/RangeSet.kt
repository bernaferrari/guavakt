package com.bernaferrari.guavakt.collect

interface RangeSet<C : Comparable<C>> {
    fun contains(value: C): Boolean
    fun rangeContaining(value: C): Range<C>?
    fun intersects(otherRange: Range<C>): Boolean = asRanges().any { current ->
        current.isConnected(otherRange) && !current.intersection(otherRange).isEmpty()
    }
    fun encloses(otherRange: Range<C>): Boolean
    fun enclosesAll(other: RangeSet<C>): Boolean = enclosesAll(other.asRanges())
    fun enclosesAll(ranges: Iterable<Range<C>>): Boolean = ranges.all(::encloses)
    fun asRanges(): Set<Range<C>>
    fun asDescendingSetOfRanges(): Set<Range<C>> = asRanges().toList().asReversed().toCollection(LinkedHashSet())
    fun isEmpty(): Boolean
    fun span(): Range<C> {
        val ranges = asRanges()
        if (ranges.isEmpty()) throw NoSuchElementException()
        return ranges.first().span(ranges.last())
    }
    fun subRangeSet(view: Range<C>): RangeSet<C> = TreeRangeSet.create<C>().also { result ->
        asRanges().forEach { current ->
            if (current.isConnected(view)) {
                val intersection = current.intersection(view)
                if (!intersection.isEmpty()) result.add(intersection)
            }
        }
    }
    fun add(range: Range<C>)
    fun addAll(other: RangeSet<C>) = addAll(other.asRanges())
    fun addAll(ranges: Iterable<Range<C>>) = ranges.forEach(::add)
    fun remove(range: Range<C>)
    fun removeAll(other: RangeSet<C>) = removeAll(other.asRanges())
    fun removeAll(ranges: Iterable<Range<C>>) = ranges.forEach(::remove)
    fun clear()
    fun complement(): RangeSet<C>

    /** Compatibility alias retained for early GuavaKt callers; Guava names this view [asRanges]. */
    @Deprecated("Use asRanges()")
    fun spans(): Set<Range<C>> = asRanges()
}
