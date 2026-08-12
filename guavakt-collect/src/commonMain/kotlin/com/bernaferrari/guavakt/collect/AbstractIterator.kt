package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.Preconditions

/** Guava AbstractIterator — compute-next pattern. */
abstract class AbstractIterator<T> : UnmodifiableIterator<T>() {
    private enum class State { READY, NOT_READY, DONE, FAILED }
    private var state = State.NOT_READY
    private var next: T? = null

    protected abstract fun computeNext(): T?

    protected fun endOfData(): T? {
        state = State.DONE
        return null
    }

    override fun hasNext(): Boolean {
        Preconditions.checkState(state != State.FAILED)
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

    fun peek(): T {
        if (!hasNext()) throw NoSuchElementException()
        @Suppress("UNCHECKED_CAST")
        return next as T
    }
}
