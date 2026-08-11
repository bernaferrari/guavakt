package dev.guavakt.util.concurrent

/** Reentrant lock/condition on JVM; cooperative single-thread lock elsewhere. */
internal expect class PlatformLock(fair: Boolean) {
    fun lock()
    fun lockInterruptibly()
    fun checkInterrupt()
    fun tryLock(): Boolean
    fun tryLockNanos(timeoutNanos: Long): Boolean
    fun tryLockNanosUninterruptibly(timeoutNanos: Long): Boolean
    fun unlock()
    fun await()
    fun awaitNanos(timeoutNanos: Long): Long
    fun awaitUninterruptibly()
    fun awaitNanosUninterruptibly(timeoutNanos: Long): Long
    fun signalAll()
    fun isHeldByCurrentThread(): Boolean
    fun holdCount(): Int
    fun isLocked(): Boolean
    fun queueLength(): Int
    fun hasQueuedThreads(): Boolean
}

internal expect fun platformThreadId(): Long
