package dev.guavakt.util.concurrent

internal actual class PlatformLock actual constructor(@Suppress("UNUSED_PARAMETER") fair: Boolean) {
    private var depth = 0
    actual fun lock() { depth++ }
    actual fun lockInterruptibly() { depth++ }
    actual fun checkInterrupt() = Unit
    actual fun tryLock(): Boolean { depth++; return true }
    actual fun tryLockNanos(timeoutNanos: Long): Boolean { depth++; return true }
    actual fun tryLockNanosUninterruptibly(timeoutNanos: Long): Boolean { depth++; return true }
    actual fun unlock() {
        if (depth <= 0) throw IllegalMonitorStateException()
        depth--
    }
    actual fun await() {
        throw UnsupportedOperationException("Blocking Monitor guards are unavailable on Kotlin/Native")
    }
    actual fun awaitNanos(timeoutNanos: Long): Long = 0L
    actual fun awaitUninterruptibly() {
        throw UnsupportedOperationException("Blocking Monitor guards are unavailable on Kotlin/Native")
    }
    actual fun awaitNanosUninterruptibly(timeoutNanos: Long): Long = 0L
    actual fun signalAll() = Unit
    actual fun isHeldByCurrentThread(): Boolean = depth > 0
    actual fun holdCount(): Int = depth
    actual fun isLocked(): Boolean = depth > 0
    actual fun queueLength(): Int = 0
    actual fun hasQueuedThreads(): Boolean = false
}

internal actual fun platformThreadId(): Long = 0L
