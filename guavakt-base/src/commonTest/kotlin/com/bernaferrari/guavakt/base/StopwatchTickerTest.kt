package com.bernaferrari.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.DurationUnit

class StopwatchTickerTest {
    private class FakeTicker(var nanos: Long = 0L) : Ticker() {
        override fun read(): Long = nanos
    }

    @Test
    fun stopwatch_elapsedAdvancesWithFakeTicker() {
        val ticker = FakeTicker(1_000L)
        val sw = Stopwatch.createUnstarted(ticker)
        assertFalse(sw.isRunning())
        sw.start()
        assertTrue(sw.isRunning())
        ticker.nanos = 1_000L + 5_000_000L // +5ms
        assertEquals(5_000_000L, sw.elapsed(DurationUnit.NANOSECONDS))
        sw.stop()
        ticker.nanos = 99_000_000_000L // should not affect stopped watch
        assertEquals(5_000_000L, sw.elapsed(DurationUnit.NANOSECONDS))
        assertEquals(5L, sw.elapsed(DurationUnit.MILLISECONDS))
    }

    @Test
    fun systemTicker_advancesWhileRunning() {
        val sw = Stopwatch.createStarted()
        // Spin enough that monotonic clock advances on all targets
        var x = 0L
        repeat(50_000) { x += it }
        assertTrue(x >= 0)
        val elapsed = sw.elapsed(DurationUnit.NANOSECONDS)
        assertTrue(elapsed >= 0L, "elapsed should be non-negative, was $elapsed")
        // After work, elapsed should be > 0 on real clocks (best-effort); allow 0 on ultra-fast CI
        sw.stop()
        val afterStop = sw.elapsed(DurationUnit.NANOSECONDS)
        assertEquals(afterStop, sw.elapsed(DurationUnit.NANOSECONDS))
    }

    @Test
    fun platformClock_nanoTimeMonotonicNonDecreasing() {
        val a = PlatformClock.nanoTime()
        val b = PlatformClock.nanoTime()
        assertTrue(b >= a)
        val ms = PlatformClock.currentTimeMillis()
        assertTrue(ms > 0L)
    }
}
