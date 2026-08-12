package com.bernaferrari.guavakt.collect

/**
 * Guava ForwardingList — forwards all list calls to [delegate].
 */
abstract class ForwardingList<E> : AbstractMutableList<E>() {
    protected abstract fun delegate(): MutableList<E>
    override val size: Int get() = delegate().size
    override fun get(index: Int): E = delegate()[index]
    override fun add(index: Int, element: E) { delegate().add(index, element) }
    override fun removeAt(index: Int): E = delegate().removeAt(index)
    override fun set(index: Int, element: E): E = delegate().set(index, element)
    override fun clear() = delegate().clear()
    override fun contains(element: E): Boolean = delegate().contains(element)
    override fun indexOf(element: E): Int = delegate().indexOf(element)
    override fun lastIndexOf(element: E): Int = delegate().lastIndexOf(element)
}
