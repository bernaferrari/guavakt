package dev.guavakt.collect

abstract class UnmodifiableIterator<E> : MutableIterator<E> {
    final override fun remove() = throw UnsupportedOperationException()
}
