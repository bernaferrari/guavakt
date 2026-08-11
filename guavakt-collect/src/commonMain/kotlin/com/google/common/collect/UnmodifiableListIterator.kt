package dev.guavakt.collect

/**
 * Guava UnmodifiableListIterator — ListIterator that rejects mutations.
 * Extends Iterator with list traversal; mutations throw UOE (Guava contract).
 */
abstract class UnmodifiableListIterator<E> : Iterator<E> {
    abstract fun hasPrevious(): Boolean
    abstract fun previous(): E
    abstract fun nextIndex(): Int
    abstract fun previousIndex(): Int

    fun remove() { throw UnsupportedOperationException() }
    fun set(element: E) { throw UnsupportedOperationException() }
    fun add(element: E) { throw UnsupportedOperationException() }
}
