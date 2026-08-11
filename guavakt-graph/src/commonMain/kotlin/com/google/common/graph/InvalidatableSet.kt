package dev.guavakt.graph

/** Guava InvalidatableSet — set implementation (LinkedHashSet storage; Guava factories). */
open class InvalidatableSet<E> protected constructor(
    protected val delegate: LinkedHashSet<E> = LinkedHashSet(),
) : AbstractMutableSet<E>() {
    override val size: Int get() = delegate.size
    override fun iterator(): MutableIterator<E> = delegate.iterator()
    override fun add(element: E): Boolean = delegate.add(element)
    override fun remove(element: E): Boolean = delegate.remove(element)
    override fun contains(element: E): Boolean = delegate.contains(element)
    override fun clear() = delegate.clear()
    companion object {
        fun <E> create(): InvalidatableSet<E> = InvalidatableSet()
        fun <E> create(elements: Collection<E>): InvalidatableSet<E> = InvalidatableSet(LinkedHashSet(elements))
        fun <E> createWithExpectedSize(expectedSize: Int): InvalidatableSet<E> =
            InvalidatableSet(LinkedHashSet(expectedSize.coerceAtLeast(0)))
    }
}
