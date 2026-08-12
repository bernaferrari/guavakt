package com.bernaferrari.guavakt.collect

/** Guava ImmutableCollection — read-only collection contract. */
abstract class ImmutableCollection<E> : Collection<E> {
    abstract override val size: Int
    abstract override fun iterator(): Iterator<E>
    override fun contains(element: E): Boolean {
        for (e in this) if (e == element) return true
        return false
    }
    override fun containsAll(elements: Collection<E>): Boolean = elements.all { contains(it) }
    override fun isEmpty(): Boolean = size == 0
    open fun asList(): ImmutableList<E> = ImmutableList.copyOf(this)
}
