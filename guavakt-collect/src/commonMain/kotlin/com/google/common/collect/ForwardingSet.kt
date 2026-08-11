package dev.guavakt.collect

/** Guava ForwardingSet — forwards all set calls to [delegate]. */
abstract class ForwardingSet<E> : AbstractMutableSet<E>() {
    protected abstract fun delegate(): MutableSet<E>
    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> = delegate().iterator()
    override fun add(element: E): Boolean = delegate().add(element)
    override fun remove(element: E): Boolean = delegate().remove(element)
    override fun clear() = delegate().clear()
    override fun contains(element: E): Boolean = delegate().contains(element)
    override fun isEmpty(): Boolean = delegate().isEmpty()
    override fun addAll(elements: Collection<E>): Boolean = delegate().addAll(elements)
    override fun removeAll(elements: Collection<E>): Boolean = delegate().removeAll(elements.toSet())
    override fun retainAll(elements: Collection<E>): Boolean = delegate().retainAll(elements.toSet())
}
