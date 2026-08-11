package dev.guavakt.collect

/** Guava CompactHashSet — set backed by [CompactHashMap]. */
open class CompactHashSet<E> private constructor(private val map: CompactHashMap<E, Boolean>) : AbstractMutableSet<E>() {
    override val size: Int get() = map.size
    override fun add(element: E): Boolean = map.put(element, true) == null
    override fun contains(element: E): Boolean = map.containsKey(element)
    override fun remove(element: E): Boolean = map.remove(element) != null
    override fun clear() = map.clear()
    override fun iterator(): MutableIterator<E> = map.keys.toMutableList().iterator()
    fun trimToSize() = map.trimToSize()

    companion object {
        fun <E> create(): CompactHashSet<E> = CompactHashSet(CompactHashMap.create())
        fun <E> create(expectedSize: Int): CompactHashSet<E> =
            CompactHashSet(CompactHashMap.create(expectedSize))
        fun <E> create(elements: Collection<E>): CompactHashSet<E> =
            create<E>().also { it.addAll(elements) }
    }
}
