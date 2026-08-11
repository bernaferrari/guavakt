package dev.guavakt.collect

/** Skeletal descending view used by custom [SortedMultiset] implementations. */
abstract class DescendingMultiset<E> : ForwardingMultiset<E>(), SortedMultiset<E> {
    protected abstract fun forwardMultiset(): SortedMultiset<E>
    final override fun delegate(): Multiset<E> = forwardMultiset()

    private fun view(): SortedMultiset<E> = DescendingSortedMultisetView(forwardMultiset())
    override fun comparator(): Comparator<in E> = view().comparator()
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<E> = view().iterator() as MutableIterator<E>
    override fun elementSet(): Set<E> = view().elementSet()
    override fun entrySet(): Set<Multiset.Entry<E>> = view().entrySet()
    override fun firstEntry(): Multiset.Entry<E>? = forwardMultiset().lastEntry()
    override fun lastEntry(): Multiset.Entry<E>? = forwardMultiset().firstEntry()
    override fun pollFirstEntry(): Multiset.Entry<E>? = forwardMultiset().pollLastEntry()
    override fun pollLastEntry(): Multiset.Entry<E>? = forwardMultiset().pollFirstEntry()
    override fun descendingMultiset(): SortedMultiset<E> = forwardMultiset()
    override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
        forwardMultiset().tailMultiset(upperBound, boundType).descendingMultiset()
    override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
        forwardMultiset().headMultiset(lowerBound, boundType).descendingMultiset()
    override fun subMultiset(
        lowerBound: E,
        lowerBoundType: BoundType,
        upperBound: E,
        upperBoundType: BoundType,
    ): SortedMultiset<E> = forwardMultiset()
        .subMultiset(upperBound, upperBoundType, lowerBound, lowerBoundType)
        .descendingMultiset()
}
