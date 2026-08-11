package dev.guavakt.util.concurrent

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

/**
 * JVM-only decorator for a real [BlockingQueue].
 *
 * Common code should use coroutine channels and suspending send/receive operations. This type is
 * intentionally absent from non-JVM targets because blocking, thread interruption, and JDK queue
 * capacity semantics cannot be reproduced honestly there.
 */
abstract class ForwardingBlockingQueue<E> : BlockingQueue<E> {
    protected abstract fun delegate(): BlockingQueue<E>

    override val size: Int get() = delegate().size

    override fun iterator(): MutableIterator<E> = delegate().iterator()

    override fun isEmpty(): Boolean = delegate().isEmpty()

    override fun contains(element: E): Boolean = delegate().contains(element)

    override fun containsAll(elements: Collection<E>): Boolean = delegate().containsAll(elements)

    override fun add(element: E): Boolean = delegate().add(element)

    override fun addAll(elements: Collection<E>): Boolean = delegate().addAll(elements)

    override fun remove(element: E): Boolean = delegate().remove(element)

    override fun removeAll(elements: Collection<E>): Boolean = delegate().removeAll(elements)

    override fun retainAll(elements: Collection<E>): Boolean = delegate().retainAll(elements)

    override fun clear() = delegate().clear()

    override fun offer(element: E): Boolean = delegate().offer(element)

    override fun poll(): E? = delegate().poll()

    override fun remove(): E = delegate().remove()

    override fun peek(): E? = delegate().peek()

    override fun element(): E = delegate().element()

    @Throws(InterruptedException::class)
    override fun put(element: E) = delegate().put(element)

    @Throws(InterruptedException::class)
    override fun offer(element: E, timeout: Long, unit: TimeUnit): Boolean =
        delegate().offer(element, timeout, unit)

    @Throws(InterruptedException::class)
    override fun take(): E = delegate().take()

    @Throws(InterruptedException::class)
    override fun poll(timeout: Long, unit: TimeUnit): E? = delegate().poll(timeout, unit)

    override fun remainingCapacity(): Int = delegate().remainingCapacity()

    override fun drainTo(target: MutableCollection<in E>): Int = delegate().drainTo(target)

    override fun drainTo(target: MutableCollection<in E>, maxElements: Int): Int =
        delegate().drainTo(target, maxElements)

    override fun toString(): String = delegate().toString()
}
