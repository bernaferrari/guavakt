package dev.guavakt.graph

/** Guava MapIteratorCache — iterator (snapshot-backed; unmodifiable remove by default). */
open class MapIteratorCache<E> protected constructor(
    private val items: List<E>,
) : Iterator<E> {
    private var index = 0
    override fun hasNext(): Boolean = index < items.size
    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()
        return items[index++]
    }
    companion object {
        fun <E> create(elements: Iterable<E>): MapIteratorCache<E> = MapIteratorCache(elements.toList())
        fun <E> empty(): MapIteratorCache<E> = MapIteratorCache(emptyList())
    }
}
