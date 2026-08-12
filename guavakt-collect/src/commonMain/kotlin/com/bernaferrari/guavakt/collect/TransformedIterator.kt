package com.bernaferrari.guavakt.collect

/**
 * Guava TransformedIterator — applies [transform] to each element of a backing iterator.
 */
abstract class TransformedIterator<F, T>(
    private val backingIterator: Iterator<F>,
) : Iterator<T> {
    protected abstract fun transform(from: F): T
    override fun hasNext(): Boolean = backingIterator.hasNext()
    override fun next(): T = transform(backingIterator.next())
}
