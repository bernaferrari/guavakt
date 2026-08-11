package dev.guavakt.util.concurrent

import kotlin.time.Duration

/**
 * A [ListeningExecutorService] with cancellable, millisecond-resolution scheduled work.
 *
 * New common code should prefer a caller-owned coroutine scope. This Guava-shaped migration API
 * uses an internal process-lifetime scheduler because [ListenableFuture] has no scope ownership
 * parameter. Negative delays and non-positive periodic intervals are rejected.
 */
interface ListeningScheduledExecutorService : ListeningExecutorService {
    fun <V> schedule(delay: Duration, task: () -> V): ListenableScheduledFuture<V>

    fun scheduleAtFixedRate(
        initialDelay: Duration,
        period: Duration,
        command: () -> Unit,
    ): ListenableScheduledFuture<Unit>

    fun scheduleWithFixedDelay(
        initialDelay: Duration,
        delay: Duration,
        command: () -> Unit,
    ): ListenableScheduledFuture<Unit>
}
