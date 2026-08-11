package dev.guavakt.concurrent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Coroutine-native guarded mutual exclusion for Kotlin Multiplatform.
 *
 * State observed by a [Guard] must be read and written only inside this monitor's guarded blocks.
 * Every completed block publishes a state change, so waiters re-evaluate their predicates without
 * polling. Cancellation while waiting is non-owning and cannot leak the mutex. No strict FIFO
 * ordering is promised.
 */
class CoroutineMonitor {
    private val mutex = Mutex()
    private val revision = MutableStateFlow(0L)

    abstract class Guard(val monitor: CoroutineMonitor) {
        abstract fun isSatisfied(): Boolean
    }

    fun newGuard(isSatisfied: () -> Boolean): Guard = object : Guard(this) {
        override fun isSatisfied(): Boolean = isSatisfied.invoke()
    }

    /** Runs [block] exclusively and notifies guarded waiters even when [block] throws. */
    suspend fun <T> withLock(block: suspend CoroutineMonitor.() -> T): T {
        mutex.lock()
        return runOwned(block)
    }

    /** Suspends until [guard] is satisfied, then runs [block] while it remains protected. */
    suspend fun <T> withLockWhen(
        guard: Guard,
        block: suspend CoroutineMonitor.() -> T,
    ): T {
        acquireWhen(guard)
        return runOwned(block)
    }

    /**
     * Waits at most [timeout] for a satisfied [guard]. The timeout does not apply to [block] once
     * the monitor is acquired. A non-positive timeout still performs one immediate check.
     */
    suspend fun tryWithLockWhen(
        guard: Guard,
        timeout: Duration,
        block: suspend CoroutineMonitor.() -> Unit,
    ): Boolean {
        checkGuard(guard)
        val acquired = if (timeout <= Duration.ZERO) {
            tryAcquireSatisfied(guard)
        } else {
            withTimeoutOrNull(timeout) {
                acquireWhen(guard)
                true
            } ?: false
        }
        if (!acquired) return false
        runOwned(block)
        return true
    }

    /** Suspends until [guard] is true, then releases the monitor without changing shared state. */
    suspend fun await(guard: Guard) {
        acquireWhen(guard)
        mutex.unlock()
    }

    fun isLocked(): Boolean = mutex.isLocked

    private suspend fun acquireWhen(guard: Guard) {
        checkGuard(guard)
        while (true) {
            val observed = revision.value
            mutex.lock()
            val satisfied = try {
                guard.isSatisfied()
            } catch (failure: Throwable) {
                mutex.unlock()
                throw failure
            }
            if (satisfied) return
            mutex.unlock()
            revision.first { it != observed }
        }
    }

    private fun tryAcquireSatisfied(guard: Guard): Boolean {
        if (!mutex.tryLock()) return false
        var satisfied = false
        try {
            satisfied = guard.isSatisfied()
            return satisfied
        } finally {
            if (!satisfied) mutex.unlock()
        }
    }

    private suspend fun <T> runOwned(block: suspend CoroutineMonitor.() -> T): T = try {
        block()
    } finally {
        revision.value = revision.value + 1L
        mutex.unlock()
    }

    private fun checkGuard(guard: Guard) {
        if (guard.monitor !== this) throw IllegalArgumentException("Guard belongs to another monitor")
    }
}
