package dev.guavakt.util.concurrent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal actual fun platformSchedule(
    delayMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle =
    schedulerScope.launch {
        delay(delayMillis.coerceAtLeast(0L))
        command()
    }.asHandle()

internal actual fun platformExecute(command: () -> Unit) {
    schedulerScope.launch { command() }
}

internal actual fun platformScheduleAtFixedRate(
    initialDelayMillis: Long,
    periodMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle {
    return schedulerScope.launch {
        val startedAt = TimeSource.Monotonic.markNow()
        var nextRunAtMillis = initialDelayMillis.coerceAtLeast(0L)
        while (true) {
            val remainingMillis = nextRunAtMillis - startedAt.elapsedNow().inWholeMilliseconds
            if (remainingMillis > 0L) delay(remainingMillis)
            command()
            if (periodMillis <= 0L) return@launch
            nextRunAtMillis = saturatedAdd(nextRunAtMillis, periodMillis)
        }
    }.asHandle()
}

internal actual fun platformScheduleWithFixedDelay(
    initialDelayMillis: Long,
    delayMillis: Long,
    command: () -> Unit,
): PlatformScheduledHandle {
    return schedulerScope.launch {
        delay(initialDelayMillis.coerceAtLeast(0L))
        command()
        if (delayMillis <= 0L) return@launch
        while (true) {
            delay(delayMillis)
            command()
        }
    }.asHandle()
}

private fun Job.asHandle(): PlatformScheduledHandle = object : PlatformScheduledHandle {
    override fun cancel() {
        this@asHandle.cancel()
    }
}

private fun saturatedAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

internal actual fun platformSleepNanosUninterruptibly(nanos: Long) { /* cooperative */ }
