package dev.guavakt.util.concurrent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTimeLimiterTest {
    @Test
    fun completesTimesOutAndOffersNullableTimeoutForm() = runTest {
        val limiter = CoroutineTimeLimiter()
        assertEquals("ready", limiter.callWithTimeout(1.seconds) { "ready" })
        assertFailsWith<CancellationException> {
            limiter.callWithTimeout(100.milliseconds) {
                delay(101.milliseconds)
                "late"
            }
        }
        assertNull(
            limiter.callWithTimeoutOrNull(100.milliseconds) {
                delay(101.milliseconds)
                "late"
            },
        )
    }

    @Test
    fun externalCancellationIsNotRewrittenAsTimeout() = runTest {
        val limiter = CoroutineTimeLimiter()
        val job = launch {
            limiter.callWithTimeout(1.seconds) { awaitCancellation() }
        }
        runCurrent()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test
    fun timeoutMustBePositiveAndBlockingShimDoesNotLie() = runTest {
        val limiter = CoroutineTimeLimiter()
        assertFailsWith<IllegalArgumentException> {
            limiter.callWithTimeout(0.milliseconds) { "never" }
        }

        val blocking = SimpleTimeLimiter.create(DirectExecutorLike { it() })
        assertFailsWith<IllegalArgumentException> { blocking.callWithTimeout({ "never" }, 0) }
        assertFailsWith<UnsupportedOperationException> { blocking.callWithTimeout({ "never" }, 1) }
    }
}
