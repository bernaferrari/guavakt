package dev.guavakt.collect

/** Guava ForwardingNavigableSet — forwards to [delegate]. */
abstract class ForwardingNavigableSet<E> : AbstractMutableSet<E>() {
    protected abstract fun delegate(): MutableSet<E>
    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> = delegate().iterator()
    override fun add(element: E): Boolean = delegate().add(element)
    override fun remove(element: E): Boolean = delegate().remove(element)
    override fun contains(element: E): Boolean = delegate().contains(element)
    override fun clear() = delegate().clear()
}
