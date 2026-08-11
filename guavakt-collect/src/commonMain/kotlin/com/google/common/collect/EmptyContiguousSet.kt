package dev.guavakt.collect

class EmptyContiguousSet<C : Comparable<C>>(private val dom: DiscreteDomain<C>) : ContiguousSet<C>() {
    override val size: Int get() = 0
    override fun iterator(): MutableIterator<C> = mutableListOf<C>().iterator()
    override fun first(): C = throw NoSuchElementException()
    override fun last(): C = throw NoSuchElementException()
    override fun range(): Range<C> = throw NoSuchElementException("Cannot get range of empty ContiguousSet")
    override fun range(lowerBoundType: BoundType, upperBoundType: BoundType): Range<C> =
        throw NoSuchElementException("Cannot get range of empty ContiguousSet")
    override fun domain(): DiscreteDomain<C> = dom
    override fun intersection(other: ContiguousSet<C>): ContiguousSet<C> = this
    override fun headSetImpl(toElement: C, inclusive: Boolean): ContiguousSet<C> = this
    override fun subSetImpl(
        fromElement: C,
        fromInclusive: Boolean,
        toElement: C,
        toInclusive: Boolean,
    ): ContiguousSet<C> = this
    override fun tailSetImpl(fromElement: C, inclusive: Boolean): ContiguousSet<C> = this
}
