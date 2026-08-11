package dev.guavakt.collect

/** Portable double-ended queue decorator backed by a [MutableList]. */
abstract class ForwardingDeque<E> : ForwardingQueue<E>() {
    protected abstract override fun delegate(): MutableList<E>

    open fun addFirst(element: E) = delegate().add(0, element)

    open fun addLast(element: E) = delegate().add(element)

    open fun descendingIterator(): MutableIterator<E> = delegate().asReversed().iterator()

    open fun getFirst(): E = delegate().first()

    open fun getLast(): E = delegate().last()

    open fun offerFirst(element: E): Boolean {
        delegate().add(0, element)
        return true
    }

    open fun offerLast(element: E): Boolean = delegate().add(element)

    open fun peekFirst(): E? = delegate().firstOrNull()

    open fun peekLast(): E? = delegate().lastOrNull()

    open fun pollFirst(): E? {
        val deque = delegate()
        return if (deque.isEmpty()) null else deque.removeAt(0)
    }

    open fun pollLast(): E? {
        val deque = delegate()
        return if (deque.isEmpty()) null else deque.removeAt(deque.lastIndex)
    }

    open fun pop(): E = removeFirst()

    open fun push(element: E) = addFirst(element)

    open fun removeFirst(): E {
        val deque = delegate()
        if (deque.isEmpty()) throw NoSuchElementException()
        return deque.removeAt(0)
    }

    open fun removeLast(): E {
        val deque = delegate()
        if (deque.isEmpty()) throw NoSuchElementException()
        return deque.removeAt(deque.lastIndex)
    }

    open fun removeFirstOccurrence(element: E): Boolean = delegate().remove(element)

    open fun removeLastOccurrence(element: E): Boolean {
        val deque = delegate()
        val index = deque.lastIndexOf(element)
        if (index < 0) return false
        deque.removeAt(index)
        return true
    }
}
