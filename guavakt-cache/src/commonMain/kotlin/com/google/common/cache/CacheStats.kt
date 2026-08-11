package dev.guavakt.cache

data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val loadSuccessCount: Long,
    val loadExceptionCount: Long,
    val totalLoadTime: Long,
    val evictionCount: Long,
) {
    fun requestCount(): Long = hitCount + missCount
    fun hitRate(): Double = if (requestCount() == 0L) 1.0 else hitCount.toDouble() / requestCount()
    fun missRate(): Double = if (requestCount() == 0L) 0.0 else missCount.toDouble() / requestCount()
}
