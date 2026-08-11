package dev.guavakt.collect

/** Guava ConcurrentHashMultiset — multiset (HashMultiset storage with Guava multiset API). */
open class ConcurrentHashMultiset<E> private constructor(
    private val backing: dev.guavakt.collect.HashMultiset<E> = dev.guavakt.collect.HashMultiset.create(),
) : AbstractMultiset<E>() {
    override val size: Int get() = backing.size
    override fun iterator(): MutableIterator<E> {
        val it = backing.iterator()
        return object : MutableIterator<E> {
            override fun hasNext() = it.hasNext()
            override fun next() = it.next()
            override fun remove() { throw UnsupportedOperationException() }
        }
    }
    override fun add(element: E): Boolean { backing.add(element, 1); return true }
    override fun count(element: Any?): Int = backing.count(element)
    override fun add(element: E, occurrences: Int): Int = backing.add(element, occurrences)
    override fun remove(element: Any?, occurrences: Int): Int = backing.remove(element, occurrences)
    override fun setCount(element: E, count: Int): Int = backing.setCount(element, count)
    override fun elementSet(): Set<E> = backing.elementSet()
    override fun entrySet(): Set<dev.guavakt.collect.Multiset.Entry<E>> = backing.entrySet()
    override fun clear() = backing.clear()
    companion object {
        fun <E> create(): ConcurrentHashMultiset<E> = ConcurrentHashMultiset()
        fun <E> create(elements: Iterable<out E>): ConcurrentHashMultiset<E> {
            val m = ConcurrentHashMultiset<E>()
            for (e in elements) m.add(e)
            return m
        }
    }
}
