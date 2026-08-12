package com.bernaferrari.guavakt.base

import kotlin.time.TimeSource

/**
 * Monotonic-ish clock for GuavaKt, implemented entirely in commonMain (single source set).
 * Resolution and epoch alignment differ by target; Guava contracts care about ordering and
 * advancement while a stopwatch is running, not bit-identical System.nanoTime().
 */
internal object PlatformClock {
    private val monotonicMark = TimeSource.Monotonic.markNow()

    /** Nanoseconds since an arbitrary fixed origin (monotonic best-effort). */
    fun nanoTime(): Long = monotonicMark.elapsedNow().inWholeNanoseconds

    /**
     * Wall-clock milliseconds best-effort from the same monotonic base plus a fixed offset
     * chosen so values are positive and increase with real time on all targets.
     * Not synchronized to Unix epoch on every platform; suitable for relative TTLs / tests via [Ticker].
     */
    fun currentTimeMillis(): Long =
        EPOCH_OFFSET_MS + monotonicMark.elapsedNow().inWholeMilliseconds

    private const val EPOCH_OFFSET_MS = 1_700_000_000_000L // ~2023-11 so values look epoch-like
}
