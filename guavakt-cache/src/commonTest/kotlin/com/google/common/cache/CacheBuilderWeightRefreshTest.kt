package dev.guavakt.cache

import dev.guavakt.base.Ticker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheBuilderWeightRefreshTest {
    private class FakeTicker(var nanos: Long = 0L) : Ticker() {
        override fun read(): Long = nanos
        fun advanceMillis(ms: Long) { nanos += ms * 1_000_000L }
    }

    @Test
    fun maximumWeight_evictsByWeigher() {
        val cache: Cache<String, String> = CacheBuilder.newBuilder<String, String>()
            .maximumWeight(10)
            .weigher { _, v -> v.length }
            .build()
        cache.put("a", "12345") // 5
        cache.put("b", "12345") // 5 -> total 10
        cache.put("c", "12345") // should evict eldest
        assertEquals(2, cache.size())
        assertNull(cache.getIfPresent("a"))
        assertEquals("12345", cache.getIfPresent("b"))
        assertEquals("12345", cache.getIfPresent("c"))
    }

    @Test
    fun typedWeigherIsAppliedLikeTheLambdaOverload() {
        val cache = CacheBuilder.newBuilder<String, String>()
            .maximumWeight(3)
            .weigher(Weigher { _, value -> value.length })
            .build<String, String>()

        cache.put("a", "aa")
        cache.put("b", "bb")

        assertEquals(null, cache.getIfPresent("a"))
        assertEquals("bb", cache.getIfPresent("b"))
    }

    @Test
    fun concurrencyLevel_retainedOnCache() {
        val cache = CacheBuilder.newBuilder<String, Int>()
            .concurrencyLevel(8)
            .build<String, Int>() as LocalCache<String, Int>
        assertEquals(8, cache.concurrencyLevel())
    }

    @Test
    fun refreshAfterWrite_reloadsWithFakeTicker() {
        val ticker = FakeTicker()
        var loads = 0
        val cache = CacheBuilder.newBuilder<String, Int>()
            .ticker(ticker)
            .refreshAfterWriteMillis(100)
            .build(CacheLoader { k -> loads++; loads })
        assertEquals(1, cache.get("k"))
        assertEquals(1, loads)
        // within refresh window — no reload on get
        ticker.advanceMillis(50)
        assertEquals(1, cache.get("k"))
        assertEquals(1, loads)
        // past refresh — reload on get
        ticker.advanceMillis(100)
        val v = cache.get("k")
        assertEquals(2, loads)
        assertEquals(2, v)
    }

    @Test
    fun millisecondTimeoutsSaturateInsteadOfOverflowing() {
        val ticker = FakeTicker()
        val expiry = CacheBuilder.newBuilder<String, String>()
            .ticker(ticker)
            .expireAfterWriteMillis(Long.MAX_VALUE)
            .build<String, String>()
        expiry.put("key", "value")
        assertEquals("value", expiry.getIfPresent("key"))
        ticker.nanos = Long.MAX_VALUE
        assertNull(expiry.getIfPresent("key"))

        val refreshTicker = FakeTicker()
        var loads = 0
        val refresh = CacheBuilder.newBuilder<String, Int>()
            .ticker(refreshTicker)
            .refreshAfterWriteMillis(Long.MAX_VALUE)
            .build(CacheLoader { ++loads })
        assertEquals(1, refresh.get("key"))
        refreshTicker.nanos = Long.MAX_VALUE
        assertEquals(2, refresh.get("key"))
    }
}
