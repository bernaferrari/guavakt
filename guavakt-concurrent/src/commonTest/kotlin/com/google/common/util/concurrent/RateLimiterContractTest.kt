package dev.guavakt.util.concurrent

import dev.guavakt.base.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class RateLimiterContractTest {
    @Test
    fun rejectedTryAcquireDoesNotConsumeCapacity() {
        val ticker = FakeTicker()
        val limiter = RateLimiter.create(2.0, ticker)

        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        ticker.nanos = 500_000_000L

        assertTrue(limiter.tryAcquire(), "the rejected attempt must not push the schedule back")
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun idleTimeAccumulatesOneSecondOfFreeBurstPermits() {
        val ticker = FakeTicker()
        val limiter = RateLimiter.create(2.0, ticker)

        assertTrue(limiter.tryAcquire())
        ticker.nanos = 1_500_000_000L

        assertTrue(limiter.tryAcquire(2), "two stored permits should be free after an idle second")
        assertTrue(limiter.tryAcquire(), "the first fresh permit is available at the same instant")
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun warmupStartsColdAndMovesTowardStableInterval() {
        val ticker = FakeTicker()
        val limiter = RateLimiter.create(2.0, 2.seconds, ticker)

        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        ticker.nanos = 1_249_999_999L
        assertFalse(limiter.tryAcquire())
        ticker.nanos = 1_250_000_000L
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun rateChangesPreserveAlreadyReservedAvailability() {
        val ticker = FakeTicker()
        val limiter = RateLimiter.create(2.0, ticker)
        assertTrue(limiter.tryAcquire())

        limiter.setRate(4.0)
        assertEquals(4.0, limiter.getRate())
        assertFalse(limiter.tryAcquire())
        ticker.nanos = 500_000_000L
        assertTrue(limiter.tryAcquire())
        ticker.nanos = 749_999_999L
        assertFalse(limiter.tryAcquire())
        ticker.nanos = 750_000_000L
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun validationAndDisplayFollowGuavaShape() {
        assertFailsWith<IllegalArgumentException> { RateLimiter.create(0.0) }
        assertFailsWith<IllegalArgumentException> { RateLimiter.create(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { RateLimiter.create(1.0).tryAcquire(0) }
        assertEquals("RateLimiter[stableRate=2.0qps]", RateLimiter.create(2.0).toString())
    }

    @Test
    fun portableBlockingShimNeverGrantsAnEarlyReservation() {
        if (platformSupportsBlockingWait()) return

        val limiter = RateLimiter.create(2.0, FakeTicker())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire(), "zero-time tryAcquire remains a truthful probe")
        assertFailsWith<UnsupportedOperationException> { limiter.acquire() }
        assertFailsWith<UnsupportedOperationException> { limiter.tryAcquire(1, 1.seconds) }
    }

    private class FakeTicker(var nanos: Long = 0L) : Ticker() {
        override fun read(): Long = nanos
    }
}
