package dev.guavakt.cache

/** A snapshot of work performed by a [CoroutineLoadingCache]. */
data class CoroutineCacheStats(
    val loadSuccessCount: Long,
    val loadFailureCount: Long,
    val loadCancellationCount: Long,
    val coalescedRequestCount: Long,
    val refreshRequestCount: Long,
    val totalLoadTimeNanos: Long,
) {
    val loadCount: Long
        get() = loadSuccessCount + loadFailureCount + loadCancellationCount

    val averageLoadTimeNanos: Double
        get() = if (loadCount == 0L) 0.0 else totalLoadTimeNanos.toDouble() / loadCount
}
