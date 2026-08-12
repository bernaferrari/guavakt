package com.bernaferrari.guavakt.base

/** Guava base AbstractIterator (compute-next). */
abstract class AbstractIterator<T> : MutableIterator<T> {
    private enum class State { READY, NOT_READY, DONE, FAILED }
    private var state = State.NOT_READY
    private var next: T? = null
    protected abstract fun computeNext(): T?
    protected fun endOfData(): T? {
        state = State.DONE
        return null
    }
    override fun hasNext(): Boolean {
        if (state == State.FAILED) throw IllegalStateException()
        return when (state) {
            State.DONE -> false
            State.READY -> true
            else -> tryToComputeNext()
        }
    }
    private fun tryToComputeNext(): Boolean {
        state = State.FAILED
        next = computeNext()
        if (state != State.DONE) {
            state = State.READY
            return true
        }
        return false
    }
    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        state = State.NOT_READY
        @Suppress("UNCHECKED_CAST")
        val result = next as T
        next = null
        return result
    }
    override fun remove() = throw UnsupportedOperationException()
}
