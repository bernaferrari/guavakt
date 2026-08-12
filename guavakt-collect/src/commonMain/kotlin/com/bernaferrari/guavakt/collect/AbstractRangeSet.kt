package com.bernaferrari.guavakt.collect

/** Guava AbstractRangeSet — set implementation (LinkedHashSet storage; Guava factories). */
open class AbstractRangeSet<E> protected constructor(
    protected val delegate: LinkedHashSet<E> = LinkedHashSet(),
) : AbstractMutableSet<E>() {
    override val size: Int get() = delegate.size
    override fun iterator(): MutableIterator<E> = delegate.iterator()
    override fun add(element: E): Boolean = delegate.add(element)
    override fun remove(element: E): Boolean = delegate.remove(element)
    override fun contains(element: E): Boolean = delegate.contains(element)
    override fun clear() = delegate.clear()
    companion object {
        fun <E> create(): AbstractRangeSet<E> = AbstractRangeSet()
        fun <E> create(elements: Collection<out E>): AbstractRangeSet<E> = AbstractRangeSet(LinkedHashSet(elements))
        fun <E> createWithExpectedSize(expectedSize: Int): AbstractRangeSet<E> =
            AbstractRangeSet(LinkedHashSet(expectedSize.coerceAtLeast(0)))
    }
}
