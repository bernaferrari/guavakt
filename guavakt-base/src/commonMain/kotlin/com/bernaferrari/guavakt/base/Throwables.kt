package com.bernaferrari.guavakt.base

/**
 * Guava Throwables utilities (KMP).
 */
object Throwables {
    inline fun <reified X : Throwable> propagateIfInstanceOf(throwable: Throwable?) {
        if (throwable is X) throw throwable
    }

    fun propagateIfPossible(throwable: Throwable?) {
        if (throwable is Error) throw throwable
        if (throwable is RuntimeException) throw throwable
    }

    fun <X : Exception> propagateIfPossible(throwable: Throwable?, declaredType: kotlin.reflect.KClass<X>) {
        if (throwable != null && declaredType.isInstance(throwable)) {
            throw throwable
        }
        propagateIfPossible(throwable)
    }

    fun getRootCause(throwable: Throwable): Throwable {
        var slowPointer: Throwable? = throwable
        var advanceSlowPointer = false
        var cause = throwable
        while (true) {
            val next = cause.cause ?: return cause
            cause = next
            if (cause === slowPointer) {
                throw IllegalArgumentException("Loop in causal chain detected.", cause)
            }
            if (advanceSlowPointer) slowPointer = slowPointer?.cause
            advanceSlowPointer = !advanceSlowPointer
        }
    }

    fun getCausalChain(throwable: Throwable): List<Throwable> {
        Preconditions.checkNotNull(throwable)
        val causes = ArrayList<Throwable>(4)
        var current: Throwable? = throwable
        while (current != null) {
            causes.add(current)
            current = current.cause
            if (current != null && causes.contains(current)) {
                throw IllegalArgumentException("Loop in causal chain detected.", current)
            }
        }
        return causes
    }

    fun getStackTraceAsString(throwable: Throwable): String = throwable.stackTraceToString()

    fun <X : Throwable> throwIfInstanceOf(throwable: Throwable, declaredType: kotlin.reflect.KClass<X>) {
        if (declaredType.isInstance(throwable)) {
            @Suppress("UNCHECKED_CAST")
            throw throwable as X
        }
    }

    fun throwIfUnchecked(throwable: Throwable) {
        propagateIfPossible(throwable)
    }
}
