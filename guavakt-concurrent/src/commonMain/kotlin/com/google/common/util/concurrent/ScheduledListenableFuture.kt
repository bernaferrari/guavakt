package dev.guavakt.util.concurrent

import kotlin.time.Duration
import kotlin.time.TimeSource

/** Internal mutable state shared by one-shot and periodic scheduled futures. */
internal class ScheduledListenableFuture<V>(delay: Duration) : AbstractFuture<V>(), ListenableScheduledFuture<V> {
    private var deadline = TimeSource.Monotonic.markNow() + delay
    private val stateLock = Any()
    private var scheduledHandle: PlatformScheduledHandle? = null
    private var activeRun: ListenableFuture<*>? = null
    private var running = false
    private var scheduleGeneration = 0L

    override fun remainingDelay(): Duration = platformMonitorSync(stateLock) { -deadline.elapsedNow() }

    fun resetDeadline(delay: Duration) {
        platformMonitorSync(stateLock) { deadline = TimeSource.Monotonic.markNow() + delay }
    }

    /** Reserves a handle slot so a late, immediate callback cannot replace a newer schedule. */
    fun reserveHandle(): Long = platformMonitorSync(stateLock) {
        scheduleGeneration += 1
        scheduleGeneration
    }

    fun installHandle(generation: Long, handle: PlatformScheduledHandle) {
        val cancel = platformMonitorSync(stateLock) {
            if (isDone() || generation != scheduleGeneration) true else {
                scheduledHandle = handle
                false
            }
        }
        if (cancel) handle.cancel()
    }

    fun completeFrom(future: ListenableFuture<out V>): Boolean = completeWithFuture(future)

    fun fail(throwable: Throwable): Boolean = completeExceptionally(throwable)

    /** Marks a periodic iteration as running, refusing overlapping runs or a completed future. */
    fun tryBeginRun(): Boolean = platformMonitorSync(stateLock) {
        if (isDone() || running) false else {
            running = true
            true
        }
    }

    /** Records the executor future so cancellation of the scheduled future reaches active work. */
    fun attachRun(future: ListenableFuture<*>): Boolean {
        val cancel = platformMonitorSync(stateLock) {
            if (isDone()) true else {
                activeRun = future
                false
            }
        }
        if (cancel) future.cancel(false)
        return !cancel
    }

    fun finishRun(future: ListenableFuture<*>) {
        platformMonitorSync(stateLock) {
            if (activeRun === future) activeRun = null
            running = false
        }
    }

    override fun afterDone() {
        val toCancel = platformMonitorSync(stateLock) {
            scheduleGeneration += 1
            val handle = scheduledHandle
            scheduledHandle = null
            val run = activeRun
            activeRun = null
            running = false
            handle to run
        }
        toCancel.first?.cancel()
        toCancel.second?.cancel(false)
    }
}
