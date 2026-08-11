package dev.guavakt.graph

/** Guava IncidentEdgeSet — set implementation (LinkedHashSet storage; Guava factories). */
open class IncidentEdgeSet<E> protected constructor(
    protected val delegate: LinkedHashSet<E> = LinkedHashSet(),
) : AbstractMutableSet<E>() {
    override val size: Int get() = delegate.size
    override fun iterator(): MutableIterator<E> = delegate.iterator()
    override fun add(element: E): Boolean = delegate.add(element)
    override fun remove(element: E): Boolean = delegate.remove(element)
    override fun contains(element: E): Boolean = delegate.contains(element)
    override fun clear() = delegate.clear()
    companion object {
        fun <E> create(): IncidentEdgeSet<E> = IncidentEdgeSet()
        fun <E> create(elements: Collection<E>): IncidentEdgeSet<E> = IncidentEdgeSet(LinkedHashSet(elements))
        fun <E> createWithExpectedSize(expectedSize: Int): IncidentEdgeSet<E> =
            IncidentEdgeSet(LinkedHashSet(expectedSize.coerceAtLeast(0)))
    }
}
