package dev.guavakt.util.concurrent

/** Guava concurrent Internal — toNanosSaturated etc. */
internal object Internal {
    fun toNanosSaturated(timeoutMillis: Long): Long {
        if (timeoutMillis <= 0) return 0
        val nanos = timeoutMillis * 1_000_000L
        return if (nanos / 1_000_000L != timeoutMillis) Long.MAX_VALUE else nanos
    }
}
