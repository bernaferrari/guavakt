package com.bernaferrari.guavakt.collect

/** Set companion to [IdentityHashMapKmp], with referential rather than value equality. */
internal class IdentityHashSetKmp<E> : AbstractMutableSet<E>() {
    private val entries = IdentityHashMapKmp<E, Unit>()

    override val size: Int get() = entries.size
    override fun contains(element: E): Boolean = entries.containsKey(element)

    override fun add(element: E): Boolean {
        if (entries.containsKey(element)) return false
        entries[element] = Unit
        return true
    }

    override fun remove(element: E): Boolean {
        if (!entries.containsKey(element)) return false
        entries.remove(element)
        return true
    }

    override fun iterator(): MutableIterator<E> {
        val iterator = entries.entries.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next().key
            override fun remove() = iterator.remove()
        }
    }

    override fun clear() = entries.clear()
}
