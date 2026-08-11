package dev.guavakt.collect

import java.util.concurrent.BlockingDeque
import java.util.concurrent.TimeUnit

/**
 * Deprecated JVM-only compatibility location for a real [BlockingDeque] decorator.
 *
 * New JVM code should use `dev.guavakt.util.concurrent.ForwardingBlockingDeque`; common code
 * should use coroutine channels rather than blocking queues.
 */
@Deprecated("Use dev.guavakt.util.concurrent.ForwardingBlockingDeque on JVM")
abstract class ForwardingBlockingDeque<E> : BlockingDeque<E> {
    protected abstract fun delegate(): BlockingDeque<E>

    override val size: Int get() = delegate().size
    override fun iterator(): MutableIterator<E> = delegate().iterator()
    override fun descendingIterator(): MutableIterator<E> = delegate().descendingIterator()
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

    override fun addFirst(element: E) = delegate().addFirst(element)
    override fun addLast(element: E) = delegate().addLast(element)
    override fun offerFirst(element: E): Boolean = delegate().offerFirst(element)
    override fun offerLast(element: E): Boolean = delegate().offerLast(element)
    override fun removeFirst(): E = delegate().removeFirst()
    override fun removeLast(): E = delegate().removeLast()
    override fun pollFirst(): E? = delegate().pollFirst()
    override fun pollLast(): E? = delegate().pollLast()
    override fun getFirst(): E = delegate().first
    override fun getLast(): E = delegate().last
    override fun peekFirst(): E? = delegate().peekFirst()
    override fun peekLast(): E? = delegate().peekLast()
    override fun removeFirstOccurrence(element: Any?): Boolean = delegate().removeFirstOccurrence(element)
    override fun removeLastOccurrence(element: Any?): Boolean = delegate().removeLastOccurrence(element)
    override fun push(element: E) = delegate().push(element)
    override fun pop(): E = delegate().pop()

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

    @Throws(InterruptedException::class)
    override fun putFirst(element: E) = delegate().putFirst(element)

    @Throws(InterruptedException::class)
    override fun putLast(element: E) = delegate().putLast(element)

    @Throws(InterruptedException::class)
    override fun offerFirst(element: E, timeout: Long, unit: TimeUnit): Boolean =
        delegate().offerFirst(element, timeout, unit)

    @Throws(InterruptedException::class)
    override fun offerLast(element: E, timeout: Long, unit: TimeUnit): Boolean =
        delegate().offerLast(element, timeout, unit)

    @Throws(InterruptedException::class)
    override fun takeFirst(): E = delegate().takeFirst()

    @Throws(InterruptedException::class)
    override fun takeLast(): E = delegate().takeLast()

    @Throws(InterruptedException::class)
    override fun pollFirst(timeout: Long, unit: TimeUnit): E? =
        delegate().pollFirst(timeout, unit)

    @Throws(InterruptedException::class)
    override fun pollLast(timeout: Long, unit: TimeUnit): E? =
        delegate().pollLast(timeout, unit)

    override fun toString(): String = delegate().toString()
}
