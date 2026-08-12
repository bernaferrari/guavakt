package com.bernaferrari.guavakt.collect

/**
 * Portable FIFO decorator backed by a [MutableList], whose index zero is the queue head.
 *
 * Kotlin common code has no `java.util.Queue`; this preserves its observable queue operations
 * while accepting the common mutable-list shape returned by GuavaKt queue factories.
 */
abstract class ForwardingQueue<E> : ForwardingCollection<E>() {
    protected abstract override fun delegate(): MutableList<E>

    open fun offer(element: E): Boolean = delegate().add(element)

    open fun poll(): E? {
        val queue = delegate()
        return if (queue.isEmpty()) null else queue.removeAt(0)
    }

    open fun remove(): E {
        val queue = delegate()
        if (queue.isEmpty()) throw NoSuchElementException()
        return queue.removeAt(0)
    }

    open fun peek(): E? = delegate().firstOrNull()

    open fun element(): E = delegate().first()

    protected fun standardOffer(element: E): Boolean =
        try {
            add(element)
        } catch (_: IllegalStateException) {
            false
        }

    protected fun standardPeek(): E? =
        try {
            element()
        } catch (_: NoSuchElementException) {
            null
        }

    protected fun standardPoll(): E? =
        try {
            remove()
        } catch (_: NoSuchElementException) {
            null
        }
}
