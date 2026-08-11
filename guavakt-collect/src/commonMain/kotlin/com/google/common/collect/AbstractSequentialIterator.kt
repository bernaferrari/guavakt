package dev.guavakt.collect

/**
 * Guava AbstractSequentialIterator — computes the next element from the previous via [computeNext].
 */
abstract class AbstractSequentialIterator<T>(
    private var nextOrNull: T?,
) : Iterator<T> {
    /** Returns the next element given the previous, or null when exhausted. */
    protected abstract fun computeNext(previous: T): T?

    override fun hasNext(): Boolean = nextOrNull != null

    override fun next(): T {
        val current = nextOrNull ?: throw NoSuchElementException()
        nextOrNull = computeNext(current)
        return current
    }
}
