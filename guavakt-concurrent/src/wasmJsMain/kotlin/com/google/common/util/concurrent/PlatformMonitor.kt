package dev.guavakt.util.concurrent

internal actual inline fun <T> platformMonitorSync(lock: Any, block: () -> T): T = block()

internal actual fun platformMonitorWait(lock: Any, timeoutMillis: Long) {
    throw UnsupportedOperationException("Blocking future waits are unavailable on Wasm; use listeners or coroutines")
}

internal actual fun platformMonitorNotifyAll(lock: Any) {
}

internal actual fun platformSupportsBlockingWait(): Boolean = false

internal actual fun platformMonitorAwaitUninterruptibly(lock: Any, condition: () -> Boolean) {
    if (!condition()) throw UnsupportedOperationException("Blocking waits are unavailable on this target")
}

internal actual fun platformMonitorAwaitUninterruptibly(
    lock: Any,
    timeoutNanos: Long,
    condition: () -> Boolean,
): Boolean = condition()
