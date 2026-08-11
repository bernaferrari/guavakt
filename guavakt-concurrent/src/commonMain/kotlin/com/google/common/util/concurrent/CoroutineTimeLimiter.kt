package dev.guavakt.util.concurrent

import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Coroutine-native replacement for blocking [TimeLimiter].
 *
 * The block runs in the caller's structured coroutine context. On expiry it is cancelled and the
 * normal `TimeoutCancellationException` is propagated; external cancellation is never converted
 * into a timeout. This cannot safely time-limit blocking or non-cooperative work.
 */
class CoroutineTimeLimiter {
    suspend fun <T> callWithTimeout(timeout: Duration, block: suspend () -> T): T {
        require(timeout.isPositive()) { "timeout must be positive: $timeout" }
        return withTimeout(timeout) { block() }
    }

    suspend fun <T> callWithTimeoutOrNull(timeout: Duration, block: suspend () -> T): T? {
        require(timeout.isPositive()) { "timeout must be positive: $timeout" }
        return withTimeoutOrNull(timeout) { block() }
    }

    suspend fun runWithTimeout(timeout: Duration, block: suspend () -> Unit) {
        callWithTimeout(timeout, block)
    }
}
