package dev.guavakt.util.concurrent

internal expect inline fun <T> platformMonitorSync(lock: Any, block: () -> T): T
internal expect fun platformMonitorWait(lock: Any, timeoutMillis: Long)
internal expect fun platformMonitorNotifyAll(lock: Any)
internal expect fun platformSupportsBlockingWait(): Boolean
internal expect fun platformMonitorAwaitUninterruptibly(lock: Any, condition: () -> Boolean)
internal expect fun platformMonitorAwaitUninterruptibly(
    lock: Any,
    timeoutNanos: Long,
    condition: () -> Boolean,
): Boolean
