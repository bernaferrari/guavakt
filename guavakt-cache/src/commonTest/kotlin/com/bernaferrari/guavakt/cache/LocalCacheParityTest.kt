package com.bernaferrari.guavakt.cache

import com.bernaferrari.guavakt.base.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalCacheParityTest {
    @Test
    fun maximumSize_one() {
        val c = CacheBuilder.newBuilder<Int, Int>().maximumSize(1).build<Int, Int>()
        c.put(1, 10)
        c.put(2, 20)
        assertEquals(1L, c.size())
        assertNull(c.getIfPresent(1))
        assertEquals(20, c.getIfPresent(2))
    }

    @Test
    fun expireAfterWriteMillis_withTicker() {
        val t = object : Ticker() {
            var n = 0L
            override fun read() = n
        }
        val c = CacheBuilder.newBuilder<String, Int>()
            .expireAfterWriteMillis(0)
            .ticker(t)
            .build<String, Int>()
        c.put("a", 1)
        assertNull(c.getIfPresent("a"))
        assertEquals(0L, c.size())
    }

    @Test
    fun expireAfterWrite_positive() {
        val t = object : Ticker() {
            var n = 0L
            override fun read() = n
        }
        val c = CacheBuilder.newBuilder<String, Int>()
            .expireAfterWriteMillis(1)
            .ticker(t)
            .build<String, Int>()
        c.put("a", 1)
        assertEquals(1, c.getIfPresent("a"))
        t.n = 2_000_000L // past 1ms in nanos
        assertNull(c.getIfPresent("a"))
    }

    @Test
    fun stats_onRecord() {
        val c = CacheBuilder.newBuilder<String, Int>().recordStats().build<String, Int>()
        c.put("x", 1)
        assertEquals(1, c.getIfPresent("x"))
        assertEquals(1L, c.stats().hitCount)
    }

    @Test
    fun asMapReadsDoNotCountAsCacheRequests() {
        val c = CacheBuilder.newBuilder<String, Int>().recordStats().build<String, Int>()
        c.put("x", 1)

        assertEquals(1, c.asMap()["x"])
        assertEquals(mapOf("x" to 1), c.asMap().toMap())

        assertEquals(0L, c.stats().hitCount)
        assertEquals(0L, c.stats().missCount)
    }
}
