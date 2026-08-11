package dev.guavakt.collect

/** Guava ForwardingMultiset — forwards multiset operations to [delegate]. */
abstract class ForwardingMultiset<E> : AbstractMultiset<E>() {
    protected abstract fun delegate(): Multiset<E>
    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> {
        val it = delegate().iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): E = it.next()
            override fun remove() { throw UnsupportedOperationException() }
        }
    }
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
    override fun clear() {
        for (e in elementSet().toList()) setCount(e, 0)
    }
}
