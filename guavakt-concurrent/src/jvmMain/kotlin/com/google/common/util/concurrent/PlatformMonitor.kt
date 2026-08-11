@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // `wait`/`notifyAll` are JVM Object-only APIs.

package dev.guavakt.util.concurrent

internal actual inline fun <T> platformMonitorSync(lock: Any, block: () -> T): T =
    synchronized(lock, block)

internal actual fun platformMonitorWait(lock: Any, timeoutMillis: Long) {
    (lock as Object).wait(timeoutMillis.coerceAtLeast(1L))
}

internal actual fun platformMonitorNotifyAll(lock: Any) {
    (lock as Object).notifyAll()
}

internal actual fun platformSupportsBlockingWait(): Boolean = true

internal actual fun platformMonitorAwaitUninterruptibly(lock: Any, condition: () -> Boolean) {
    var interrupted = false
    try {
        synchronized(lock) {
            while (!condition()) {
                try {
                    (lock as Object).wait()
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        }
    } finally {
        if (interrupted) Thread.currentThread().interrupt()
    }
}

internal actual fun platformMonitorAwaitUninterruptibly(
    lock: Any,
    timeoutNanos: Long,
    condition: () -> Boolean,
): Boolean {
    val boundedTimeout = timeoutNanos.coerceAtLeast(0L)
    val startedAt = System.nanoTime()
    var interrupted = false
    try {
        synchronized(lock) {
            var remaining = boundedTimeout
            while (!condition()) {
                if (remaining <= 0L) return false
                try {
                    val millis = remaining / 1_000_000L
                    val nanos = (remaining % 1_000_000L).toInt()
                    (lock as Object).wait(millis, nanos)
                } catch (_: InterruptedException) {
                    interrupted = true
                }
                val elapsed = System.nanoTime() - startedAt
                remaining = if (elapsed <= 0L) boundedTimeout else (boundedTimeout - elapsed).coerceAtLeast(0L)
            }
            return true
        }
    } finally {
        if (interrupted) Thread.currentThread().interrupt()
    }
}
