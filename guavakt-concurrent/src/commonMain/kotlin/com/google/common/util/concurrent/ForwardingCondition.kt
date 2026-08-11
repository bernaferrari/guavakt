package dev.guavakt.util.concurrent

/** Guava ForwardingCondition — condition variable forwarding. */
abstract class ForwardingCondition {
    protected abstract fun delegate(): ConditionLike
    fun await() = delegate().await()
    fun signal() = delegate().signal()
    fun signalAll() = delegate().signalAll()
}

interface ConditionLike {
    fun await()
    fun signal()
    fun signalAll()
}
