package dev.guavakt.cache

import dev.guavakt.base.Ticker
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalCacheConcurrencyTest {
    @Test fun concurrentGetsCoalesceOneLoad() {
        val loads = AtomicInteger()
        val cache = CacheBuilder.newBuilder<String, Int>().build(CacheLoader {
            loads.incrementAndGet()
            Thread.sleep(30)
            42
        })
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = (0 until 8).map {
                executor.submit<Int> { start.await(); cache.get("key") }
            }
            start.countDown()
            assertEquals(List(8) { 42 }, results.map { it.get(5, TimeUnit.SECONDS) })
            assertEquals(1, loads.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test fun warmLoadingGetCountsOneHitAndFailedLoadTime() {
        val ticker = object : Ticker() {
            var value = 0L
            override fun read(): Long = value
        }
        val cache = CacheBuilder.newBuilder<String, Int>().ticker(ticker).recordStats().build(CacheLoader { key ->
            ticker.value += 7
            if (key == "bad") error("boom")
            1
        })
        cache.get("ok")
        cache.get("ok")
        runCatching { cache.get("bad") }
        val stats = cache.stats()
        assertEquals(1, stats.hitCount)
        assertEquals(2, stats.missCount)
        assertEquals(1, stats.loadSuccessCount)
        assertEquals(1, stats.loadExceptionCount)
        assertTrue(stats.totalLoadTime >= 14)
    }
}
