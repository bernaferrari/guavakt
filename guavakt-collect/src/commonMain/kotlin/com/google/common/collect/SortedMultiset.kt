package dev.guavakt.collect

/**
 * A multiset whose distinct elements iterate in comparator order.
 *
 * Common Kotlin has no `NavigableSet`, so [elementSet] is a live ordered [Set];
 * navigation is expressed by the range and boundary-entry operations here.
 */
interface SortedMultiset<E> : SortedMultisetBridge<E>, SortedIterable<E> {
    override fun comparator(): Comparator<in E>

    fun firstEntry(): Multiset.Entry<E>? = entrySet().firstOrNull()
    fun lastEntry(): Multiset.Entry<E>? = entrySet().lastOrNull()

    fun pollFirstEntry(): Multiset.Entry<E>? {
        val iterator = entrySet().iterator() as? MutableIterator<Multiset.Entry<E>>
            ?: throw UnsupportedOperationException()
        if (!iterator.hasNext()) return null
        val result = iterator.next().let { Multisets.immutableEntry(it.getElement(), it.getCount()) }
        iterator.remove()
        return result
    }

    fun pollLastEntry(): Multiset.Entry<E>? {
        val entry = lastEntry() ?: return null
        val result = Multisets.immutableEntry(entry.getElement(), entry.getCount())
        setCount(entry.getElement(), 0)
        return result
    }

    fun descendingMultiset(): SortedMultiset<E> = DescendingSortedMultisetView(this)
    fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E>
    fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E>

    fun subMultiset(
        lowerBound: E,
        lowerBoundType: BoundType,
        upperBound: E,
        upperBoundType: BoundType,
    ): SortedMultiset<E> {
        require(comparator().compare(lowerBound, upperBound) <= 0) {
            "lowerBound ($lowerBound) must be <= upperBound ($upperBound)"
        }
        return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType)
    }
}
