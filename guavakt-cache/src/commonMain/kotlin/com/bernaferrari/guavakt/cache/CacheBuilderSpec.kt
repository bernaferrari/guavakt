package com.bernaferrari.guavakt.cache

import com.bernaferrari.guavakt.base.Preconditions

/**
 * Guava CacheBuilderSpec — parses Guava-style config strings.
 * Keys: maximumSize, expireAfterWrite, expireAfterAccess, weakKeys, weakValues, softValues, recordStats.
 */
class CacheBuilderSpec private constructor(
    val maximumSize: Long?,
    val expireAfterWriteMillis: Long?,
    val expireAfterAccessMillis: Long?,
    val weakKeys: Boolean,
    val weakValues: Boolean,
    val softValues: Boolean,
    val recordStats: Boolean,
) {
    fun toCacheBuilder(): CacheBuilder<Any, Any> {
        val b = CacheBuilder.newBuilder<Any, Any>()
        maximumSize?.let { b.maximumSize(it) }
        expireAfterWriteMillis?.let { b.expireAfterWriteMillis(it) }
        expireAfterAccessMillis?.let { b.expireAfterAccessMillis(it) }
        if (weakKeys) b.weakKeys()
        if (weakValues) b.weakValues()
        if (softValues) b.softValues()
        if (recordStats) b.recordStats()
        return b
    }

    companion object {
        fun parse(cacheBuilderSpecification: String): CacheBuilderSpec {
            Preconditions.checkNotNull(cacheBuilderSpecification)
            var maximumSize: Long? = null
            var expireAfterWriteMillis: Long? = null
            var expireAfterAccessMillis: Long? = null
            var weakKeys = false
            var weakValues = false
            var softValues = false
            var recordStats = false
            if (cacheBuilderSpecification.isBlank()) {
                return CacheBuilderSpec(null, null, null, false, false, false, false)
            }
            for (part in cacheBuilderSpecification.split(',')) {
                val piece = part.trim()
                if (piece.isEmpty()) continue
                val eq = piece.indexOf('=')
                val key = if (eq == -1) piece else piece.substring(0, eq).trim()
                val value = if (eq == -1) "" else piece.substring(eq + 1).trim()
                when (key) {
                    "maximumSize" -> maximumSize = value.toLong()
                    "expireAfterWrite" -> expireAfterWriteMillis = parseDurationMillis(value)
                    "expireAfterAccess" -> expireAfterAccessMillis = parseDurationMillis(value)
                    "weakKeys" -> weakKeys = true
                    "weakValues" -> weakValues = true
                    "softValues" -> softValues = true
                    "recordStats" -> recordStats = true
                }
            }
            return CacheBuilderSpec(
                maximumSize, expireAfterWriteMillis, expireAfterAccessMillis,
                weakKeys, weakValues, softValues, recordStats,
            )
        }

        private fun parseDurationMillis(spec: String): Long {
            val num = buildString { for (c in spec) if (c.isDigit()) append(c) }.toLongOrNull() ?: 0L
            return when {
                spec.endsWith("ms") -> num
                spec.endsWith("s") -> num * 1000
                spec.endsWith("m") -> num * 60_000
                spec.endsWith("h") -> num * 3_600_000
                spec.endsWith("d") -> num * 86_400_000
                else -> num
            }
        }
    }
}
