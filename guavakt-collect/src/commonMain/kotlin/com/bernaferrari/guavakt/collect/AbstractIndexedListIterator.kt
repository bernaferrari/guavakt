package com.bernaferrari.guavakt.collect

/** Guava AbstractIndexedListIterator — iterator (snapshot-backed; unmodifiable remove by default). */
open class AbstractIndexedListIterator<E> protected constructor(
    private val items: List<E>,
) : Iterator<E> {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()
        return items[index++]
    }
    companion object {
        fun <E> create(elements: Iterable<E>): AbstractIndexedListIterator<E> = AbstractIndexedListIterator(elements.toList())
        fun <E> empty(): AbstractIndexedListIterator<E> = AbstractIndexedListIterator(emptyList())
    }
}
