package dev.guavakt.collect

/** Guava CompactLinkedHashSet — insertion-ordered set. */
open class CompactLinkedHashSet<E> private constructor(
    private val map: CompactLinkedHashMap<E, Boolean>,
) : AbstractMutableSet<E>() {
    override val size: Int get() = map.size
    override fun add(element: E): Boolean = map.put(element, true) == null
    override fun contains(element: E): Boolean = map.containsKey(element)
    override fun remove(element: E): Boolean = map.remove(element) != null
    override fun clear() = map.clear()
    override fun iterator(): MutableIterator<E> = map.keys.toMutableList().iterator()
    fun trimToSize() = map.trimToSize()

    companion object {
        fun <E> create(): CompactLinkedHashSet<E> = CompactLinkedHashSet(CompactLinkedHashMap.create())
        fun <E> create(expectedSize: Int): CompactLinkedHashSet<E> =
            CompactLinkedHashSet(CompactLinkedHashMap.create(expectedSize))
        fun <E> create(elements: Collection<E>): CompactLinkedHashSet<E> =
            create<E>().also { it.addAll(elements) }
    }
}
