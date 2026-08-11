package dev.guavakt.util.concurrent

import kotlin.time.Duration

/**
 * A [ListenableFuture] whose work has been scheduled for a future time.
 *
 * [remainingDelay] uses the target's monotonic clock and can become negative once execution is
 * eligible. Periodic futures remain incomplete until they are cancelled or an iteration fails.
 */
interface ListenableScheduledFuture<V> : ListenableFuture<V> {
    fun remainingDelay(): Duration
}
