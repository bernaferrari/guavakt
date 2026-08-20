package com.bernaferrari.guavakt.collect

/**
 * A [MutableList] with deque operations. It uses Kotlin's circular [ArrayDeque], so adding and
 * removing at either end is efficient while ordinary indexed list operations remain available.
 */
class QueueList<E>() : AbstractMutableList<E>() {
    private val elements = ArrayDeque<E>()

    constructor(values: Iterable<E>) : this() {
        elements.addAll(values)
    }

    override val size: Int get() = elements.size
    override fun get(index: Int): E = elements[index]
    override fun set(index: Int, element: E): E = elements.set(index, element)
    override fun add(index: Int, element: E) = elements.add(index, element)
    override fun removeAt(index: Int): E = elements.removeAt(index)

    fun addFirst(element: E) = elements.addFirst(element)
    fun addLast(element: E) = elements.addLast(element)
    fun firstOrNull(): E? = elements.firstOrNull()
    fun lastOrNull(): E? = elements.lastOrNull()
    fun removeFirstOrNull(): E? = elements.removeFirstOrNull()
    fun removeLastOrNull(): E? = elements.removeLastOrNull()

    override fun clear() = elements.clear()
}
