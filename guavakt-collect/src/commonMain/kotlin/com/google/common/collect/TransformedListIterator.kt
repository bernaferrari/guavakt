package dev.guavakt.collect

/** Guava TransformedListIterator — iterator (snapshot-backed; unmodifiable remove by default). */
open class TransformedListIterator<E> protected constructor(
    private val items: List<E>,
) : Iterator<E> {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()
        return items[index++]
    }
    companion object {
        fun <E> create(elements: Iterable<E>): TransformedListIterator<E> = TransformedListIterator(elements.toList())
        fun <E> empty(): TransformedListIterator<E> = TransformedListIterator(emptyList())
    }
}
