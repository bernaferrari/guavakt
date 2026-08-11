package dev.guavakt.collect

/** Portable sorted set (Guava TreeSet iteration order). */
open class ComparatorTreeSet<E>(private val cmp: Comparator<in E>?) : AbstractMutableSet<E>() {
    private val map = ComparatorTreeMap<E, Boolean>(cmp)

    fun comparator(): Comparator<in E>? = cmp

    override val size: Int get() = map.size

    override fun add(element: E): Boolean = map.put(element, true) == null

    override fun contains(element: E): Boolean = map.containsKey(element)

    override fun remove(element: E): Boolean = map.remove(element) != null

    override fun clear() = map.clear()

    override fun iterator(): MutableIterator<E> {
        val keys = map.keys.toMutableList()
        return object : MutableIterator<E> {
            private val it = keys.iterator()
            private var last: E? = null
            override fun hasNext() = it.hasNext()
            override fun next(): E {
                last = it.next()
                return last!!
            }
            override fun remove() {
                val e = last ?: throw IllegalStateException()
                map.remove(e)
                it.remove()
                last = null
            }
        }
    }
}
