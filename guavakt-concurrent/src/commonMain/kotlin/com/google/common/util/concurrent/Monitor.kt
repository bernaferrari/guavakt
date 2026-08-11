package dev.guavakt.util.concurrent

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Thrown when a guard belongs to another monitor or the caller does not occupy the monitor. */
class IllegalMonitorStateException(message: String? = null) : IllegalStateException(message)

/**
 * A reentrant mutual-exclusion monitor with explicit [Guard] conditions.
 *
 * JVM operations have Guava's blocking, timeout, reentrancy, interruption, and interrupt-restoring
 * semantics. JavaScript, Wasm, and Native expose the same immediate/reentrant operations, but an
 * indefinite wait for an unsatisfied guard throws [UnsupportedOperationException]; timed waits
 * return `false` immediately. Use [CoroutineMonitor] for portable suspending coordination.
 *
 * This implementation uses one condition and selective guard checks rather than one JVM condition
 * per guard. It may wake more threads than Guava, but preserves the observable safety contract:
 * predicates are evaluated while occupied, signals are not lost, and a successful guarded entry
 * returns with both the monitor occupied and its guard satisfied.
 */
class Monitor(private val fair: Boolean = false) {
    private val lock = PlatformLock(fair)
    // Guava pushes newly-active guards at the head, which also defines predicate-check order.
    private val activeGuards = ArrayList<Guard>()

    abstract class Guard(val monitor: Monitor) {
        internal var waiterCount: Int = 0

        /** Retained for source compatibility; monitor internals no longer thread guards through it. */
        @Deprecated("Monitor manages active guards internally")
        @kotlin.concurrent.Volatile
        var next: Guard? = null

        abstract fun isSatisfied(): Boolean

        @Deprecated("Call monitor.enter() so lock ownership remains explicit")
        fun enter() = monitor.enter()

        @Deprecated("Call monitor.leave() so lock ownership remains explicit")
        fun leave() = monitor.leave()
    }

    /** Creates a guard whose predicate is always evaluated while this monitor is occupied. */
    fun newGuard(isSatisfied: () -> Boolean): Guard = object : Guard(this) {
        override fun isSatisfied(): Boolean = isSatisfied.invoke()
    }

    fun enter() = lock.lock()

    /** Uninterruptibly attempts to enter within [timeout], including pre-existing interrupts. */
    fun enter(timeout: Duration): Boolean {
        if (!fair && lock.tryLock()) return true
        return lock.tryLockNanosUninterruptibly(timeout.toSafeNanos())
    }

    fun enterInterruptibly() = lock.lockInterruptibly()

    fun enterInterruptibly(timeout: Duration): Boolean =
        lock.tryLockNanos(timeout.toSafeNanos())

    /** Immediate entry; like Guava, this deliberately ignores fairness. */
    fun tryEnter(): Boolean = lock.tryLock()

    fun enterWhen(guard: Guard) {
        checkGuard(guard)
        val signalBeforeWaiting = lock.isHeldByCurrentThread()
        lock.lockInterruptibly()
        var satisfied = false
        try {
            if (!isSatisfied(guard)) await(guard, signalBeforeWaiting)
            satisfied = true
        } finally {
            if (!satisfied) unlockAfterFailedEntry()
        }
    }

    fun enterWhen(guard: Guard, timeout: Duration): Boolean {
        checkGuard(guard)
        val timeoutNanos = timeout.toSafeNanos()
        val startedAt = startTimer(timeoutNanos)
        val reentrant = lock.isHeldByCurrentThread()
        if (!lock.tryLockNanos(timeoutNanos)) return false

        var satisfied = false
        try {
            satisfied = isSatisfied(guard) ||
                awaitNanos(guard, remainingNanos(startedAt, timeoutNanos), reentrant, false)
            return satisfied
        } finally {
            if (!satisfied) unlockAfterFailedEntry()
        }
    }

    fun enterWhenUninterruptibly(guard: Guard) {
        checkGuard(guard)
        val signalBeforeWaiting = lock.isHeldByCurrentThread()
        lock.lock()
        var satisfied = false
        try {
            if (!isSatisfied(guard)) awaitUninterruptibly(guard, signalBeforeWaiting)
            satisfied = true
        } finally {
            if (!satisfied) unlockAfterFailedEntry()
        }
    }

    fun enterWhenUninterruptibly(guard: Guard, timeout: Duration): Boolean {
        checkGuard(guard)
        val timeoutNanos = timeout.toSafeNanos()
        val startedAt = startTimer(timeoutNanos)
        val reentrant = lock.isHeldByCurrentThread()
        if (!lock.tryLockNanosUninterruptibly(timeoutNanos)) return false

        var satisfied = false
        try {
            satisfied = isSatisfied(guard) ||
                awaitNanos(guard, remainingNanos(startedAt, timeoutNanos), reentrant, true)
            return satisfied
        } finally {
            if (!satisfied) unlockAfterFailedEntry()
        }
    }

    /** Acquires the monitor, checks [guard] once, and never waits for the guard itself. */
    fun enterIf(guard: Guard): Boolean {
        checkGuard(guard)
        lock.lock()
        return keepLockOnlyIfSatisfied(guard)
    }

    fun enterIf(guard: Guard, timeout: Duration): Boolean {
        checkGuard(guard)
        if (!enter(timeout)) return false
        return keepLockOnlyIfSatisfied(guard)
    }

    fun enterIfInterruptibly(guard: Guard): Boolean {
        checkGuard(guard)
        lock.lockInterruptibly()
        return keepLockOnlyIfSatisfied(guard)
    }

    fun enterIfInterruptibly(guard: Guard, timeout: Duration): Boolean {
        checkGuard(guard)
        if (!lock.tryLockNanos(timeout.toSafeNanos())) return false
        return keepLockOnlyIfSatisfied(guard)
    }

    fun tryEnterIf(guard: Guard): Boolean {
        checkGuard(guard)
        if (!lock.tryLock()) return false
        return keepLockOnlyIfSatisfied(guard)
    }

    @Deprecated("Guava calls this non-waiting operation tryEnterIf", ReplaceWith("tryEnterIf(guard)"))
    fun tryEnterWhen(guard: Guard): Boolean = tryEnterIf(guard)

    /** Waits interruptibly for [guard]; the caller must already occupy this monitor. */
    fun waitFor(guard: Guard) {
        checkGuardAndOccupied(guard)
        if (!isSatisfied(guard)) await(guard, true)
    }

    fun waitFor(guard: Guard, timeout: Duration): Boolean {
        checkGuardAndOccupied(guard)
        if (isSatisfied(guard)) return true
        lock.checkInterrupt()
        return awaitNanos(guard, timeout.toSafeNanos(), true, false)
    }

    fun waitForUninterruptibly(guard: Guard) {
        checkGuardAndOccupied(guard)
        if (!isSatisfied(guard)) awaitUninterruptibly(guard, true)
    }

    fun waitForUninterruptibly(guard: Guard, timeout: Duration): Boolean {
        checkGuardAndOccupied(guard)
        if (isSatisfied(guard)) return true
        return awaitNanos(guard, timeout.toSafeNanos(), true, true)
    }

    /** Leaves once, signalling a satisfied active guard only at the outermost reentrant depth. */
    fun leave() {
        if (!lock.isHeldByCurrentThread()) {
            throw IllegalMonitorStateException("Current thread does not occupy this monitor")
        }
        try {
            if (lock.holdCount() == 1) signalSatisfiedWaiters()
        } finally {
            lock.unlock()
        }
    }

    fun isFair(): Boolean = fair
    fun isOccupied(): Boolean = lock.isLocked()
    fun isOccupiedByCurrentThread(): Boolean = lock.isHeldByCurrentThread()
    fun getOccupiedDepth(): Int = lock.holdCount()
    fun getQueueLength(): Int = lock.queueLength()
    fun hasQueuedThreads(): Boolean = lock.hasQueuedThreads()
    fun hasWaiters(guard: Guard): Boolean = getWaitQueueLength(guard) > 0

    fun getWaitQueueLength(guard: Guard): Int {
        checkGuard(guard)
        lock.lock()
        return try {
            guard.waiterCount
        } finally {
            lock.unlock()
        }
    }

    private fun keepLockOnlyIfSatisfied(guard: Guard): Boolean {
        var satisfied = false
        try {
            satisfied = isSatisfied(guard)
            return satisfied
        } finally {
            if (!satisfied) lock.unlock()
        }
    }

    private fun await(guard: Guard, signalBeforeWaiting: Boolean) {
        if (signalBeforeWaiting) signalSatisfiedWaiters()
        beginWaitingFor(guard)
        try {
            do {
                lock.await()
            } while (!isSatisfied(guard))
        } finally {
            endWaitingFor(guard)
        }
    }

    private fun awaitUninterruptibly(guard: Guard, signalBeforeWaiting: Boolean) {
        if (signalBeforeWaiting) signalSatisfiedWaiters()
        beginWaitingFor(guard)
        try {
            do {
                lock.awaitUninterruptibly()
            } while (!isSatisfied(guard))
        } finally {
            endWaitingFor(guard)
        }
    }

    private fun awaitNanos(
        guard: Guard,
        timeoutNanos: Long,
        signalBeforeWaiting: Boolean,
        uninterruptible: Boolean,
    ): Boolean {
        var remaining = timeoutNanos
        var registered = false
        try {
            while (true) {
                if (remaining <= 0L) return false
                if (!registered) {
                    if (signalBeforeWaiting) signalSatisfiedWaiters()
                    beginWaitingFor(guard)
                    registered = true
                }
                remaining = if (uninterruptible) {
                    lock.awaitNanosUninterruptibly(remaining)
                } else {
                    lock.awaitNanos(remaining)
                }
                if (isSatisfied(guard)) return true
            }
        } finally {
            if (registered) endWaitingFor(guard)
        }
    }

    private fun beginWaitingFor(guard: Guard) {
        if (guard.waiterCount++ == 0) activeGuards.add(0, guard)
    }

    private fun endWaitingFor(guard: Guard) {
        guard.waiterCount--
        if (guard.waiterCount == 0) activeGuards -= guard
    }

    private fun signalSatisfiedWaiters() {
        for (guard in activeGuards) {
            if (isSatisfied(guard)) {
                // A shared condition requires waking all waiters; each rechecks its own guard.
                lock.signalAll()
                return
            }
        }
    }

    private fun isSatisfied(guard: Guard): Boolean = try {
        guard.isSatisfied()
    } catch (failure: Throwable) {
        lock.signalAll()
        throw failure
    }

    private fun checkGuard(guard: Guard) {
        if (guard.monitor !== this) throw IllegalMonitorStateException("Guard belongs to another monitor")
    }

    private fun checkGuardAndOccupied(guard: Guard) {
        if (guard.monitor !== this || !lock.isHeldByCurrentThread()) {
            throw IllegalMonitorStateException("Current thread does not occupy this guard's monitor")
        }
    }

    private fun unlockAfterFailedEntry() {
        try {
            signalSatisfiedWaiters()
        } finally {
            lock.unlock()
        }
    }

    private fun startTimer(timeoutNanos: Long): TimeMark? =
        if (timeoutNanos <= 0L) null else TimeSource.Monotonic.markNow()

    private fun remainingNanos(startedAt: TimeMark?, timeoutNanos: Long): Long {
        if (startedAt == null) return 0L
        return (timeoutNanos - startedAt.elapsedNow().inWholeNanoseconds).coerceAtLeast(0L)
    }

    private fun Duration.toSafeNanos(): Long {
        if (this <= Duration.ZERO) return 0L
        return inWholeNanoseconds.coerceAtMost(MONITOR_MAX_SAFE_NANOS)
    }
}

private const val MONITOR_MAX_SAFE_NANOS: Long = (Long.MAX_VALUE / 4L) * 3L
