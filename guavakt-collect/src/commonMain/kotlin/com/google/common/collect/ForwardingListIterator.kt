package dev.guavakt.collect

/** Guava ForwardingListIterator — forwards to [delegate]. */
abstract class ForwardingListIterator<E> : AbstractMutableCollection<E>() {
    protected abstract fun delegate(): MutableCollection<E>
    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> = delegate().iterator()
    override fun add(element: E): Boolean = delegate().add(element)
    override fun remove(element: E): Boolean = delegate().remove(element)
    override fun clear() = delegate().clear()
}
