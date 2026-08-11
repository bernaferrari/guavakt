package dev.guavakt.util.concurrent

import java.util.concurrent.BlockingDeque
import java.util.concurrent.TimeUnit

/** JVM-only decorator for a real [BlockingDeque]; use coroutine channels in common code. */
abstract class ForwardingBlockingDeque<E> : ForwardingBlockingQueue<E>(), BlockingDeque<E> {
    protected abstract override fun delegate(): BlockingDeque<E>

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

    override fun descendingIterator(): MutableIterator<E> = delegate().descendingIterator()

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
}
