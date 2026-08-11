package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

@GwtCompatible(emulated = true)
class Stopwatch private constructor(private val ticker: Ticker) {
    private var isRunning: Boolean = false
    private var elapsedNanos: Long = 0
    private var startTick: Long = 0

    fun isRunning(): Boolean = isRunning

    fun start(): Stopwatch {
        Preconditions.checkState(!isRunning, "This stopwatch is already running.")
        isRunning = true
        startTick = ticker.read()
        return this
    }

    fun stop(): Stopwatch {
        val tick = ticker.read()
        Preconditions.checkState(isRunning, "This stopwatch is already stopped.")
        isRunning = false
        elapsedNanos += tick - startTick
        return this
    }

    fun reset(): Stopwatch {
        elapsedNanos = 0
        isRunning = false
        return this
    }

    private fun elapsedNanos(): Long =
        if (isRunning) ticker.read() - startTick + elapsedNanos else elapsedNanos

    fun elapsed(unit: DurationUnit): Long = elapsed().toLong(unit)

    fun elapsed(): Duration = elapsedNanos().nanoseconds

    override fun toString(): String {
        val nanos = elapsedNanos()
        val duration = nanos.nanoseconds
        val unit = chooseUnit(nanos)
        val value = duration.toDouble(unit)
        return "${formatCompact4Digits(value)} ${abbreviate(unit)}"
    }

    companion object {
        fun createUnstarted(): Stopwatch = Stopwatch(Ticker.systemTicker())
        fun createUnstarted(ticker: Ticker): Stopwatch = Stopwatch(Preconditions.checkNotNull(ticker))
        fun createStarted(): Stopwatch = createUnstarted().start()
        fun createStarted(ticker: Ticker): Stopwatch = createUnstarted(ticker).start()

        private fun chooseUnit(nanos: Long): DurationUnit = when {
            nanos >= 86_400_000_000_000L -> DurationUnit.DAYS
            nanos >= 3_600_000_000_000L -> DurationUnit.HOURS
            nanos >= 60_000_000_000L -> DurationUnit.MINUTES
            nanos >= 1_000_000_000L -> DurationUnit.SECONDS
            nanos >= 1_000_000L -> DurationUnit.MILLISECONDS
            nanos >= 1_000L -> DurationUnit.MICROSECONDS
            else -> DurationUnit.NANOSECONDS
        }

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        private fun abbreviate(unit: DurationUnit): String = when (unit) {
            DurationUnit.NANOSECONDS -> "ns"
            DurationUnit.MICROSECONDS -> "μs"
            DurationUnit.MILLISECONDS -> "ms"
            DurationUnit.SECONDS -> "s"
            DurationUnit.MINUTES -> "min"
            DurationUnit.HOURS -> "h"
            DurationUnit.DAYS -> "d"
            // DurationUnit is expect/actual in Kotlin's common metadata. Keep this
            // branch so metadata compilation remains valid if Kotlin adds a unit.
            else -> unit.name.lowercase()
        }

        private fun formatCompact4Digits(value: Double): String {
            val s = value.toString()
            return if (s.length <= 6) s else s.take(6)
        }
    }
}
