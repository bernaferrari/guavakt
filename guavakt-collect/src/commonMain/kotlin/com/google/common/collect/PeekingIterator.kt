package dev.guavakt.collect

interface PeekingIterator<E> : Iterator<E> {
    fun peek(): E
}
