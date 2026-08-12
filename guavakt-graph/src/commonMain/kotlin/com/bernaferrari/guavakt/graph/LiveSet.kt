package com.bernaferrari.guavakt.graph

/**
 * A read-only set whose contents are computed from mutable graph storage on every operation.
 *
 * The wrapper deliberately exposes only Kotlin's read-only [Set] surface even when its backing
 * collection is mutable. Relation views can supply [isValid] so a removed node or edge fails on
 * later use, matching Guava's invalidatable-view contract.
 */
internal class LiveSet<E>(
    private val isValid: () -> Boolean = { true },
    private val invalid: () -> String = { "This graph view is no longer valid" },
    private val values: () -> Set<E>,
) : AbstractSet<E>() {
    override val size: Int
        get() = current().size

    override fun contains(element: E): Boolean = current().contains(element)

    override fun iterator(): Iterator<E> {
        val delegate = current().iterator()
        // Do not leak a MutableIterator from mutable backing storage through a Kotlin Set view.
        return object : Iterator<E> {
            override fun hasNext(): Boolean = delegate.hasNext()
            override fun next(): E = delegate.next()
        }
    }

    private fun current(): Set<E> {
        check(isValid()) { invalid() }
        return values()
    }
}
