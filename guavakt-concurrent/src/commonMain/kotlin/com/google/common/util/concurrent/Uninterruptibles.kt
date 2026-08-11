package dev.guavakt.util.concurrent

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Guava Uninterruptibles — operations that complete even if interrupted (restore interrupt flag).
 * Blocking calls are a JVM migration facility. Common code can use these only for already-complete
 * futures; use [await] for cancellable, non-blocking waiting.
 */
object Uninterruptibles {
    @kotlin.concurrent.Volatile private var interruptedFlag = false

    fun sleepUninterruptibly(sleepFor: Long, unitMillis: Boolean = true) {
        if (sleepFor <= 0) return
        requireBlockingWait("sleep")
        val nanos = if (unitMillis) {
            if (sleepFor > Long.MAX_VALUE / 1_000_000L) Long.MAX_VALUE else sleepFor * 1_000_000L
        } else {
            sleepFor
        }
        platformSleepNanosUninterruptibly(nanos)
    }

    fun <V> getUninterruptibly(future: ListenableFuture<V>): V {
        var wasInterrupted = false
        try {
            while (true) {
                try {
                    return future.get()
                } catch (_: InterruptedExceptionLike) {
                    wasInterrupted = true
                }
            }
        } finally {
            if (wasInterrupted) interruptedFlag = true
        }
    }

    fun <V> getUninterruptibly(future: ListenableFuture<V>, timeoutMillis: Long): V {
        require(timeoutMillis >= 0L) { "timeoutMillis must not be negative: $timeoutMillis" }
        if (future.isDone()) return getUninterruptibly(future)
        requireBlockingWait("timed Future.get")

        val timeout = timeoutMillis.milliseconds
        val startedAt = TimeSource.Monotonic.markNow()
        while (!future.isDone()) {
            val remaining = timeout - startedAt.elapsedNow()
            if (remaining <= kotlin.time.Duration.ZERO) {
                throw TimeoutException("Timed out after ${timeoutMillis}ms")
            }
            platformSleepNanosUninterruptibly(
                minOf(remaining.inWholeNanoseconds.coerceAtLeast(1L), 1_000_000L),
            )
        }
        return getUninterruptibly(future)
    }

    fun joinUninterruptibly(threadJoin: () -> Unit) {
        var wasInterrupted = false
        try {
            while (true) {
                try {
                    threadJoin()
                    return
                } catch (_: InterruptedExceptionLike) {
                    wasInterrupted = true
                }
            }
        } finally {
            if (wasInterrupted) interruptedFlag = true
        }
    }

    fun awaitUninterruptibly(conditionAwait: () -> Boolean): Boolean {
        var wasInterrupted = false
        try {
            while (true) {
                try {
                    return conditionAwait()
                } catch (_: InterruptedExceptionLike) {
                    wasInterrupted = true
                }
            }
        } finally {
            if (wasInterrupted) interruptedFlag = true
        }
    }

    fun tryAcquireUninterruptibly(acquire: () -> Boolean): Boolean {
        var wasInterrupted = false
        try {
            while (true) {
                try {
                    return acquire()
                } catch (_: InterruptedExceptionLike) {
                    wasInterrupted = true
                }
            }
        } finally {
            if (wasInterrupted) interruptedFlag = true
        }
    }

    private fun requireBlockingWait(operation: String) {
        if (!platformSupportsBlockingWait()) {
            throw UnsupportedOperationException(
                "$operation is unavailable without blocking threads; use coroutine await/delay instead",
            )
        }
    }
}

/** Marker used by KMP ports in place of java.lang.InterruptedException. */
open class InterruptedExceptionLike(message: String? = null) : Exception(message)
