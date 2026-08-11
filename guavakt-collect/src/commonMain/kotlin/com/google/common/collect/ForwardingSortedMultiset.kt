package dev.guavakt.collect

/** Guava ForwardingSortedMultiset — forwards all sorted-multiset operations. */
abstract class ForwardingSortedMultiset<E> : AbstractMultiset<E>(), SortedMultiset<E> {
    protected abstract fun delegate(): SortedMultiset<E>
    override val size: Int get() = delegate().size
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<E> = delegate().iterator() as MutableIterator<E>
    override fun add(element: E): Boolean {
        delegate().add(element, 1)
        return true
    }
    override fun count(element: Any?): Int = delegate().count(element)
    override fun add(element: E, occurrences: Int): Int = delegate().add(element, occurrences)
    override fun remove(element: Any?, occurrences: Int): Int = delegate().remove(element, occurrences)
    override fun setCount(element: E, count: Int): Int = delegate().setCount(element, count)
    override fun elementSet(): Set<E> = delegate().elementSet()
    override fun entrySet(): Set<Multiset.Entry<E>> = delegate().entrySet()
    override fun comparator(): Comparator<in E> = delegate().comparator()
    override fun firstEntry(): Multiset.Entry<E>? = delegate().firstEntry()
    override fun lastEntry(): Multiset.Entry<E>? = delegate().lastEntry()
    override fun pollFirstEntry(): Multiset.Entry<E>? = delegate().pollFirstEntry()
    override fun pollLastEntry(): Multiset.Entry<E>? = delegate().pollLastEntry()
    override fun descendingMultiset(): SortedMultiset<E> = delegate().descendingMultiset()
    override fun headMultiset(upperBound: E, boundType: BoundType): SortedMultiset<E> =
        delegate().headMultiset(upperBound, boundType)
    override fun tailMultiset(lowerBound: E, boundType: BoundType): SortedMultiset<E> =
        delegate().tailMultiset(lowerBound, boundType)
    override fun subMultiset(
        lowerBound: E,
        lowerBoundType: BoundType,
        upperBound: E,
        upperBoundType: BoundType,
    ): SortedMultiset<E> = delegate().subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType)
    override fun clear() {
        for (element in elementSet().toList()) setCount(element, 0)
    }
}
