package dev.guavakt.collect

/** Guava ConsumingQueueIterator — iterator (snapshot-backed; unmodifiable remove by default). */
open class ConsumingQueueIterator<E> protected constructor(
    private val items: List<E>,
) : Iterator<E> {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()
        return items[index++]
    }
    companion object {
        fun <E> create(elements: Iterable<E>): ConsumingQueueIterator<E> = ConsumingQueueIterator(elements.toList())
        fun <E> empty(): ConsumingQueueIterator<E> = ConsumingQueueIterator(emptyList())
    }
}
