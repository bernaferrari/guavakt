package com.bernaferrari.guavakt.collect

/** Portable equivalent of Guava's package-private `Range.rangeLexOrdering()`. */
internal fun <C : Comparable<C>> rangeLexComparator(): Comparator<Range<C>> = Comparator { first, second ->
    compareLowerBounds(first, second).takeIf { it != 0 } ?: compareUpperBounds(first, second)
}

private fun <C : Comparable<C>> compareLowerBounds(first: Range<C>, second: Range<C>): Int {
    if (!first.hasLowerBound()) return if (!second.hasLowerBound()) 0 else -1
    if (!second.hasLowerBound()) return 1
    val endpoint = first.lowerEndpoint().compareTo(second.lowerEndpoint())
    if (endpoint != 0) return endpoint
    if (first.lowerBoundType() == second.lowerBoundType()) return 0
    return if (first.lowerBoundType() == BoundType.CLOSED) -1 else 1
}

private fun <C : Comparable<C>> compareUpperBounds(first: Range<C>, second: Range<C>): Int {
    if (!first.hasUpperBound()) return if (!second.hasUpperBound()) 0 else 1
    if (!second.hasUpperBound()) return -1
    val endpoint = first.upperEndpoint().compareTo(second.upperEndpoint())
    if (endpoint != 0) return endpoint
    if (first.upperBoundType() == second.upperBoundType()) return 0
    return if (first.upperBoundType() == BoundType.OPEN) -1 else 1
}
