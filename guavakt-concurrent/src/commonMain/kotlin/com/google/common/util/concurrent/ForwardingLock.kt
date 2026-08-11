package dev.guavakt.util.concurrent

/** Guava ForwardingLock — forwards to a delegate lock. */
abstract class ForwardingLock {
    protected abstract fun delegate(): LockLike
    fun lock() = delegate().lock()
    fun unlock() = delegate().unlock()
    fun tryLock(): Boolean = delegate().tryLock()
    fun isHeldByCurrentThread(): Boolean = delegate().isHeldByCurrentThread()
}

interface LockLike {
    fun lock()
    fun unlock()
    fun tryLock(): Boolean
    fun isHeldByCurrentThread(): Boolean
}
