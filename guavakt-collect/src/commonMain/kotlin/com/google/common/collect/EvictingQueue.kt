package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava EvictingQueue — bounded queue that evicts oldest elements when full.
 */
class EvictingQueue<E> private constructor(
    private val maxSize: Int,
    private val delegate: ArrayDeque<E> = ArrayDeque(),
) : AbstractMutableCollection<E>(), MutableCollection<E> {
    override val size: Int get() = delegate.size

    override fun add(element: E): Boolean {
        Preconditions.checkNotNull(element)
        if (maxSize == 0) return true
        if (size == maxSize) delegate.removeFirst()
        delegate.addLast(element)
        return true
    }

    override fun iterator(): MutableIterator<E> = delegate.iterator()

    fun remainingCapacity(): Int = maxSize - size

    fun peek(): E? = delegate.firstOrNull()

    fun element(): E = delegate.first()

    fun poll(): E? = if (delegate.isEmpty()) null else delegate.removeFirst()

    fun remove(): E = delegate.removeFirst()

    fun offer(e: E): Boolean = add(e)

    companion object {
        fun <E> create(maxSize: Int): EvictingQueue<E> {
            Preconditions.checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize)
            return EvictingQueue(maxSize)
        }
    }
}
