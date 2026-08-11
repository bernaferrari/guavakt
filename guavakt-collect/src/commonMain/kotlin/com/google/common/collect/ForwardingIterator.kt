package dev.guavakt.collect

/** Guava ForwardingIterator — forwards to [delegate]. */
abstract class ForwardingIterator<E> : AbstractMutableCollection<E>() {
    protected abstract fun delegate(): MutableCollection<E>
    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> = delegate().iterator()
    override fun add(element: E): Boolean = delegate().add(element)
    override fun remove(element: E): Boolean = delegate().remove(element)
    override fun clear() = delegate().clear()
}
