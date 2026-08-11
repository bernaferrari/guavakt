package dev.guavakt.parity

import com.google.common.util.concurrent.GuavaRateLimiterHarness
import dev.guavakt.base.Ticker
import dev.guavakt.util.concurrent.RateLimiter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class RateLimiterDifferentialTest {
    @Test
    fun burstyAvailabilityTraceMatchesGuava() {
        val guava = GuavaRateLimiterHarness.bursty(2.0)
        val kotlinTicker = FakeTicker()
        val kotlin = RateLimiter.create(2.0, kotlinTicker)

        compareTry(guava, kotlin, 1)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 499_999)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 1)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 1_500_000)
        compareTry(guava, kotlin, 2)
        compareTry(guava, kotlin, 1)
        compareTry(guava, kotlin, 1)
    }

    @Test
    fun warmingUpAvailabilityTraceMatchesGuava() {
        val guava = GuavaRateLimiterHarness.warmingUp(2.0, 2_000_000)
        val kotlinTicker = FakeTicker()
        val kotlin = RateLimiter.create(2.0, 2.seconds, kotlinTicker)

        compareTry(guava, kotlin, 1)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 1_249_999)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 1)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 1_000_000)
        compareTry(guava, kotlin, 1)
    }

    @Test
    fun rateChangeAndPresentationMatchGuava() {
        val guava = GuavaRateLimiterHarness.bursty(2.0)
        val kotlinTicker = FakeTicker()
        val kotlin = RateLimiter.create(2.0, kotlinTicker)

        assertEquals(guava.toString(), kotlin.toString())
        compareTry(guava, kotlin, 1)
        guava.setRate(4.0)
        kotlin.setRate(4.0)
        assertEquals(guava.getRate(), kotlin.getRate())
        assertEquals(guava.toString(), kotlin.toString())
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 500_000)
        compareTry(guava, kotlin, 1)
        advance(guava, kotlinTicker, 250_000)
        compareTry(guava, kotlin, 1)
    }

    @Test
    fun validationAndNegativeTimeoutMatchGuava() {
        assertFailsWith<IllegalArgumentException> { GuavaRateLimiterHarness.bursty(0.0) }
        assertFailsWith<IllegalArgumentException> { RateLimiter.create(0.0) }
        assertFailsWith<IllegalArgumentException> { GuavaRateLimiterHarness.bursty(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { RateLimiter.create(Double.NaN) }

        val guava = GuavaRateLimiterHarness.bursty(1.0)
        val kotlin = RateLimiter.create(1.0, FakeTicker())
        assertEquals(guava.tryAcquire(1, -1), kotlin.tryAcquire(1, -1))
        assertEquals(guava.tryAcquire(1, -1), kotlin.tryAcquire(1, -1))
        assertFailsWith<IllegalArgumentException> { guava.tryAcquire(0) }
        assertFailsWith<IllegalArgumentException> { kotlin.tryAcquire(0) }
    }

    private fun compareTry(
        guava: GuavaRateLimiterHarness,
        kotlin: RateLimiter,
        permits: Int,
    ) {
        assertEquals(guava.tryAcquire(permits), kotlin.tryAcquire(permits))
    }

    private fun advance(
        guava: GuavaRateLimiterHarness,
        ticker: FakeTicker,
        micros: Long,
    ) {
        guava.advanceMicros(micros)
        ticker.nanos += micros * 1_000L
    }

    private class FakeTicker(var nanos: Long = 0L) : Ticker() {
        override fun read(): Long = nanos
    }
}
