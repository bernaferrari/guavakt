package dev.guavakt.util.concurrent

/**
 * Schedule [command] after [delayMillis].
 *
 * Each target provides a real delayed callback: the JVM uses its daemon scheduler and the other
 * targets use a private coroutine scheduler.  The scheduler is deliberately internal: public
 * common code should own its coroutine scope rather than inherit this process-lifetime work.
 */
internal expect fun platformSchedule(delayMillis: Long, command: () -> Unit): PlatformScheduledHandle

/** Dispatch work away from the lifecycle caller. It is intentionally not structured public work. */
internal expect fun platformExecute(command: () -> Unit)

/** Schedule [command] after [initialDelayMillis], then every [periodMillis] until cancelled. */
internal expect fun platformScheduleAtFixedRate(
    initialDelayMillis: Long,
    periodMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle

/** Schedule [command] after [initialDelayMillis], waiting [delayMillis] after each completion. */
internal expect fun platformScheduleWithFixedDelay(
    initialDelayMillis: Long,
    delayMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle

internal interface PlatformScheduledHandle {
    fun cancel()
}

/** Guava-style uninterruptible monotonic sleep; only JVM can provide real blocking. */
internal expect fun platformSleepNanosUninterruptibly(nanos: Long)
