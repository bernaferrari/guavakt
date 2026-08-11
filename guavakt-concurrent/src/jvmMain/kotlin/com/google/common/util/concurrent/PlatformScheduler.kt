package dev.guavakt.util.concurrent

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "guavakt-scheduler").apply { isDaemon = true }
}

private val executionWorkers = Executors.newCachedThreadPool { r ->
    Thread(r, "guavakt-execution").apply { isDaemon = true }
}

internal actual fun platformSchedule(
    delayMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle =
    scheduler.schedule(command, delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS).asHandle()

internal actual fun platformExecute(command: () -> Unit) {
    executionWorkers.execute(command)
}

internal actual fun platformScheduleAtFixedRate(
    initialDelayMillis: Long,
    periodMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle {
    val future: ScheduledFuture<*> = if (periodMillis <= 0) {
        scheduler.schedule(command, initialDelayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
    } else {
        scheduler.scheduleAtFixedRate(
            command,
            initialDelayMillis.coerceAtLeast(0),
            periodMillis,
            TimeUnit.MILLISECONDS,
        )
    }
    return future.asHandle()
}

internal actual fun platformScheduleWithFixedDelay(
    initialDelayMillis: Long,
    delayMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle {
    val future: ScheduledFuture<*> = if (delayMillis <= 0) {
        scheduler.schedule(command, initialDelayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
    } else {
        scheduler.scheduleWithFixedDelay(
            command,
            initialDelayMillis.coerceAtLeast(0),
            delayMillis,
            TimeUnit.MILLISECONDS,
        )
    }
    return future.asHandle()
}

private fun ScheduledFuture<*>.asHandle(): PlatformScheduledHandle = object : PlatformScheduledHandle {
    override fun cancel() {
        cancel(false)
    }
}

internal actual fun platformSleepNanosUninterruptibly(nanos: Long) {
    if (nanos <= 0L) return
    val startedAt = System.nanoTime()
    var interrupted = false
    try {
        var remaining = nanos
        while (remaining > 0L) {
            try {
                Thread.sleep(remaining / 1_000_000L, (remaining % 1_000_000L).toInt())
            } catch (_: InterruptedException) {
                interrupted = true
            }
            val elapsed = System.nanoTime() - startedAt
            remaining = if (elapsed <= 0L) nanos else (nanos - elapsed).coerceAtLeast(0L)
        }
    } finally {
        if (interrupted) Thread.currentThread().interrupt()
    }
}
