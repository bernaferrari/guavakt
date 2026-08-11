package dev.guavakt.util.concurrent

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal actual class PlatformLock actual constructor(fair: Boolean) {
    private val lock = ReentrantLock(fair)
    private val condition = lock.newCondition()

    actual fun lock() = lock.lock()
    actual fun lockInterruptibly() = lock.lockInterruptibly()
    actual fun checkInterrupt() {
        if (Thread.interrupted()) throw InterruptedException()
    }
    actual fun tryLock(): Boolean = lock.tryLock()
    actual fun tryLockNanos(timeoutNanos: Long): Boolean =
        lock.tryLock(timeoutNanos.coerceAtLeast(0L), TimeUnit.NANOSECONDS)

    actual fun tryLockNanosUninterruptibly(timeoutNanos: Long): Boolean {
        val timeout = timeoutNanos.coerceAtLeast(0L)
        val startedAt = System.nanoTime()
        var interrupted = Thread.interrupted()
        try {
            var remaining = timeout
            while (true) {
                try {
                    return lock.tryLock(remaining, TimeUnit.NANOSECONDS)
                } catch (_: InterruptedException) {
                    interrupted = true
                    remaining = remainingNanos(startedAt, timeout)
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    actual fun unlock() = lock.unlock()
    actual fun await() = condition.await()
    actual fun awaitNanos(timeoutNanos: Long): Long = condition.awaitNanos(timeoutNanos)
    actual fun awaitUninterruptibly() = condition.awaitUninterruptibly()

    actual fun awaitNanosUninterruptibly(timeoutNanos: Long): Long {
        val timeout = timeoutNanos.coerceAtLeast(0L)
        val startedAt = System.nanoTime()
        var interrupted = Thread.interrupted()
        try {
            var remaining = timeout
            while (true) {
                try {
                    return condition.awaitNanos(remaining)
                } catch (_: InterruptedException) {
                    interrupted = true
                    remaining = remainingNanos(startedAt, timeout)
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    actual fun signalAll() = condition.signalAll()
    actual fun isHeldByCurrentThread(): Boolean = lock.isHeldByCurrentThread
    actual fun holdCount(): Int = lock.holdCount
    actual fun isLocked(): Boolean = lock.isLocked
    actual fun queueLength(): Int = lock.queueLength
    actual fun hasQueuedThreads(): Boolean = lock.hasQueuedThreads()

    private fun remainingNanos(startedAt: Long, timeoutNanos: Long): Long {
        val elapsed = System.nanoTime() - startedAt
        return if (elapsed <= 0L) timeoutNanos else (timeoutNanos - elapsed).coerceAtLeast(0L)
    }
}

internal actual fun platformThreadId(): Long = Thread.currentThread().id
