package com.bernaferrari.guavakt.parity

import com.google.common.cache.CacheBuilder as GuavaCacheBuilder
import com.google.common.cache.CacheLoader as GuavaCacheLoader
import com.google.common.cache.RemovalListener as GuavaRemovalListener
import com.google.common.cache.Weigher as GuavaWeigher
import com.google.common.base.Ticker as GuavaTicker
import com.bernaferrari.guavakt.base.Ticker
import com.bernaferrari.guavakt.cache.CacheBuilder
import com.bernaferrari.guavakt.cache.CacheLoader
import com.bernaferrari.guavakt.cache.RemovalListener
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class CacheDifferentialTest {
    private class GuavaFakeTicker(var nanos: Long = 0L) : GuavaTicker() {
        override fun read(): Long = nanos
    }

    private class KotlinFakeTicker(var nanos: Long = 0L) : Ticker() {
        override fun read(): Long = nanos
    }

    @Test fun maximumSizeLruAndRequestStatsMatchGuava() {
        val guava = GuavaCacheBuilder.newBuilder()
            .maximumSize(2)
            .recordStats()
            .build<String, Int>()
        val ours = CacheBuilder.newBuilder<String, Int>()
            .maximumSize(2)
            .recordStats()
            .build<String, Int>()

        for ((key, value) in listOf("a" to 1, "b" to 2)) {
            guava.put(key, value)
            ours.put(key, value)
        }
        assertEquals(guava.getIfPresent("a"), ours.getIfPresent("a"))
        guava.put("c", 3)
        ours.put("c", 3)
        assertEquals(guava.getIfPresent("missing"), ours.getIfPresent("missing"))

        assertEquals(guava.asMap().toMap(), ours.asMap().toMap())
        assertEquals(guava.stats().hitCount(), ours.stats().hitCount)
        assertEquals(guava.stats().missCount(), ours.stats().missCount)
        assertEquals(guava.stats().evictionCount(), ours.stats().evictionCount)
    }

    @Test
    fun loadingCacheGetAllDuplicateKeysMatchesGuava() {
        var guavaLoads = 0
        val guava = GuavaCacheBuilder.newBuilder()
            .recordStats()
            .build(object : GuavaCacheLoader<String, Int>() {
                override fun load(key: String): Int = ++guavaLoads
            })
        var kotlinLoads = 0
        val kotlin = CacheBuilder.newBuilder<String, Int>()
            .recordStats()
            .build(CacheLoader { ++kotlinLoads })
        val keys = listOf("b", "a", "b", "c", "a")

        assertEquals(guava.getAll(keys), kotlin.getAll(keys))
        assertEquals(guavaLoads, kotlinLoads, "distinct misses must load once each")
        assertEquals(guava.asMap().toMap(), kotlin.asMap().toMap())
        assertEquals(guava.stats().hitCount(), kotlin.stats().hitCount)
        assertEquals(guava.stats().missCount(), kotlin.stats().missCount)
        assertEquals(guava.stats().loadSuccessCount(), kotlin.stats().loadSuccessCount)
        assertEquals(guava.stats().loadExceptionCount(), kotlin.stats().loadExceptionCount)

        assertEquals(guava.getAll(listOf("b", "fresh", "b", "fresh")), kotlin.getAll(listOf("b", "fresh", "b", "fresh")))
        assertEquals(guavaLoads, kotlinLoads)
        assertEquals(guava.stats().hitCount(), kotlin.stats().hitCount)
        assertEquals(guava.stats().missCount(), kotlin.stats().missCount)
        assertEquals(guava.stats().loadSuccessCount(), kotlin.stats().loadSuccessCount)
    }

    @Test
    fun loadingCacheCustomBulkLoaderMatchesGuavaIncludingExtrasOrderingAndStats() {
        val guavaRequests = mutableListOf<List<String>>()
        val guava = GuavaCacheBuilder.newBuilder()
            .recordStats()
            .build(object : GuavaCacheLoader<String, Int>() {
                override fun load(key: String): Int = error("single-key fallback should not run")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> {
                    guavaRequests += keys.toList()
                    return buildMap {
                        keys.forEach { put(it, it.length) }
                        put("extra", 99)
                    }
                }
            })
        val kotlinRequests = mutableListOf<List<String>>()
        val kotlin = CacheBuilder.newBuilder<String, Int>()
            .recordStats()
            .build(object : CacheLoader<String, Int> {
                override fun load(key: String): Int = error("single-key fallback should not run")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> {
                    kotlinRequests += keys.toList()
                    return buildMap {
                        keys.forEach { put(it, it.length) }
                        put("extra", 99)
                    }
                }
            })
        guava.put("hit", 7)
        kotlin.put("hit", 7)
        val keys = listOf("cold", "hit", "fresh", "cold")

        assertEquals(guava.getAll(keys), kotlin.getAll(keys))
        assertEquals(guava.getAll(keys).keys.toList(), kotlin.getAll(keys).keys.toList())
        assertEquals(guavaRequests, kotlinRequests)
        assertEquals(guava.asMap().toMap(), kotlin.asMap().toMap())
        assertEquals(guava.stats().hitCount(), kotlin.stats().hitCount)
        assertEquals(guava.stats().missCount(), kotlin.stats().missCount)
        assertEquals(guava.stats().loadSuccessCount(), kotlin.stats().loadSuccessCount)
        assertEquals(guava.stats().loadExceptionCount(), kotlin.stats().loadExceptionCount)
    }

    @Test
    fun loadingCacheIncompleteBulkResultCachesValidEntriesThenFailsLikeGuava() {
        val guava = GuavaCacheBuilder.newBuilder()
            .recordStats()
            .build(object : GuavaCacheLoader<String, Int>() {
                override fun load(key: String): Int = error("single-key fallback should not run")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> = mapOf("first" to 1, "extra" to 9)
            })
        val kotlin = CacheBuilder.newBuilder<String, Int>()
            .recordStats()
            .build(object : CacheLoader<String, Int> {
                override fun load(key: String): Int = error("single-key fallback should not run")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> = mapOf("first" to 1, "extra" to 9)
            })

        assertEquals(
            failureName { guava.getAll(listOf("first", "missing")) },
            failureName { kotlin.getAll(listOf("first", "missing")) },
        )
        assertEquals(guava.asMap().toMap(), kotlin.asMap().toMap())
        assertEquals(guava.stats().missCount(), kotlin.stats().missCount)
        assertEquals(guava.stats().loadSuccessCount(), kotlin.stats().loadSuccessCount)
        assertEquals(guava.stats().loadExceptionCount(), kotlin.stats().loadExceptionCount)
    }

    @Test
    fun loadingCacheNullBulkEntriesCacheValidSiblingsThenFailLikeGuava() {
        val guava = GuavaCacheBuilder.newBuilder()
            .recordStats()
            .build(object : GuavaCacheLoader<String, Int>() {
                override fun load(key: String): Int = error("single-key fallback should not run")
                @Suppress("UNCHECKED_CAST")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> =
                    linkedMapOf<String, Int?>("one" to 1, "bad" to null) as Map<String, Int>
            })
        val kotlin = CacheBuilder.newBuilder<String, Int>()
            .recordStats()
            .build(object : CacheLoader<String, Int> {
                override fun load(key: String): Int = error("single-key fallback should not run")
                @Suppress("UNCHECKED_CAST")
                override fun loadAll(keys: Iterable<String>): Map<String, Int> =
                    linkedMapOf<String, Int?>("one" to 1, "bad" to null) as Map<String, Int>
            })

        assertEquals(
            failureName { guava.getAll(listOf("one")) },
            failureName { kotlin.getAll(listOf("one")) },
        )
        assertEquals(guava.asMap().toMap(), kotlin.asMap().toMap())
        assertEquals(guava.stats().missCount(), kotlin.stats().missCount)
        assertEquals(guava.stats().loadSuccessCount(), kotlin.stats().loadSuccessCount)
        assertEquals(guava.stats().loadExceptionCount(), kotlin.stats().loadExceptionCount)
    }

    @Test
    fun extremeMillisecondExpirySaturatesLikeGuava() {
        val guavaTicker = GuavaFakeTicker()
        val kotlinTicker = KotlinFakeTicker()
        val guava = GuavaCacheBuilder.newBuilder()
            .ticker(guavaTicker)
            .expireAfterWrite(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
            .build<String, String>()
        val kotlin = CacheBuilder.newBuilder<String, String>()
            .ticker(kotlinTicker)
            .expireAfterWriteMillis(Long.MAX_VALUE)
            .build<String, String>()

        guava.put("key", "value")
        kotlin.put("key", "value")
        assertEquals(guava.getIfPresent("key"), kotlin.getIfPresent("key"))
        guavaTicker.nanos = Long.MAX_VALUE
        kotlinTicker.nanos = Long.MAX_VALUE
        assertEquals(guava.getIfPresent("key"), kotlin.getIfPresent("key"))
    }

    @Test
    fun cacheBuilderSingleAssignmentFailuresMatchGuava() {
        assertEquals(
            listOf(
                failureName { GuavaCacheBuilder.newBuilder().maximumSize(1).maximumSize(2) },
                failureName { GuavaCacheBuilder.newBuilder().maximumSize(1).maximumWeight(1) },
                failureName { GuavaCacheBuilder.newBuilder().maximumSize(1).weigher(GuavaWeigher<Any, Any> { _, _ -> 1 }) },
                failureName { GuavaCacheBuilder.newBuilder().weakValues().softValues() },
                failureName { GuavaCacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MILLISECONDS).expireAfterWrite(2, TimeUnit.MILLISECONDS) },
                failureName { GuavaCacheBuilder.newBuilder().refreshAfterWrite(0, TimeUnit.MILLISECONDS) },
                failureName { GuavaCacheBuilder.newBuilder().ticker(GuavaFakeTicker()).ticker(GuavaFakeTicker()) },
            ),
            listOf(
                failureName { CacheBuilder.newBuilder<Any, Any>().maximumSize(1).maximumSize(2) },
                failureName { CacheBuilder.newBuilder<Any, Any>().maximumSize(1).maximumWeight(1) },
                failureName { CacheBuilder.newBuilder<Any, Any>().maximumSize(1).weigher { _, _ -> 1 } },
                failureName { CacheBuilder.newBuilder<Any, Any>().weakValues().softValues() },
                failureName { CacheBuilder.newBuilder<Any, Any>().expireAfterWriteMillis(1).expireAfterWriteMillis(2) },
                failureName { CacheBuilder.newBuilder<Any, Any>().refreshAfterWriteMillis(0) },
                failureName { CacheBuilder.newBuilder<Any, Any>().ticker(KotlinFakeTicker()).ticker(KotlinFakeTicker()) },
            ),
        )
    }

    @Test
    fun zeroExpiryActsAsZeroSizeCacheLikeGuava() {
        val guavaRemovals = mutableListOf<String>()
        val guava = GuavaCacheBuilder.newBuilder()
            .expireAfterWrite(0, TimeUnit.NANOSECONDS)
            .removalListener(GuavaRemovalListener<Any, Any> { notification ->
                guavaRemovals += "${notification.key}:${notification.value}:${notification.cause}"
            })
            .build<String, String>()
        val kotlinRemovals = mutableListOf<String>()
        val kotlin = CacheBuilder.newBuilder<String, String>()
            .expireAfterWriteMillis(0)
            .removalListener(RemovalListener { notification ->
                kotlinRemovals += "${notification.getKey()}:${notification.getValue()}:${notification.cause}"
            })
            .build<String, String>()

        guava.put("key", "value")
        kotlin.put("key", "value")
        assertEquals(guava.getIfPresent("key"), kotlin.getIfPresent("key"))
        assertEquals(guava.size(), kotlin.size())
        assertEquals(guavaRemovals, kotlinRemovals)
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
