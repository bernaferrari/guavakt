package dev.guavakt.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CacheTest {
    @Test
    fun getIfAbsent_loadsOnce() {
        var loads = 0
        val cache = CacheBuilder.newBuilder<String, Int>().recordStats().build(
            CacheLoader { k -> loads++; k.length }
        )
        assertEquals(5, cache.get("hello"))
        assertEquals(5, cache.get("hello"))
        assertEquals(1, loads)
        assertTrue(cache.stats().hitCount >= 1)
        assertTrue(cache.stats().loadSuccessCount >= 1)
        assertEquals(1, cache.stats().missCount, "cold LoadingCache.get must count exactly one miss")
    }

    @Test
    fun getWithLoader_missCountIsOne() {
        val cache: Cache<String, Int> = CacheBuilder.newBuilder<String, Int>().recordStats().build()
        assertEquals(3, cache.get("abc") { "abc".length })
        assertEquals(1, cache.stats().missCount)
        assertEquals(1, cache.stats().loadSuccessCount)
        assertEquals(3, cache.get("abc") { error("should not reload") })
        assertEquals(1, cache.stats().missCount)
        assertEquals(1, cache.stats().hitCount)
    }

    @Test
    fun maximumSize_evictsEldest() {
        val cache: Cache<Int, Int> = CacheBuilder.newBuilder<Int, Int>().maximumSize(2).build()
        cache.put(1, 10)
        cache.put(2, 20)
        cache.put(3, 30)
        assertEquals(2, cache.size())
        assertNull(cache.getIfPresent(1))
        assertEquals(20, cache.getIfPresent(2))
    }

    @Test
    fun customBulkLoaderCachesExtrasPreservesRequestOrderAndValidatesResponses() {
        val requests = mutableListOf<List<String>>()
        val cache = CacheBuilder.newBuilder<String, Int>().recordStats().build(object : CacheLoader<String, Int> {
            override fun load(key: String): Int = error("single-key fallback should not run")
            override fun loadAll(keys: Iterable<String>): Map<String, Int> {
                requests += keys.toList()
                return buildMap {
                    keys.forEach { put(it, it.length) }
                    put("extra", 99)
                }
            }
        })
        cache.put("hit", 7)

        assertEquals(linkedMapOf("cold" to 4, "hit" to 7, "fresh" to 5), cache.getAll(listOf("cold", "hit", "fresh", "cold")))
        assertEquals(listOf(listOf("cold", "fresh")), requests)
        assertEquals(99, cache.getIfPresent("extra"))
        assertEquals(1, cache.stats().loadSuccessCount)
        assertEquals(2, cache.stats().missCount)

        val incomplete = CacheBuilder.newBuilder<String, Int>().build(object : CacheLoader<String, Int> {
            override fun load(key: String): Int = error("single-key fallback should not run")
            override fun loadAll(keys: Iterable<String>): Map<String, Int> = mapOf("one" to 1, "extra" to 9)
        })
        assertFailsWith<CacheLoader.InvalidCacheLoadException> { incomplete.getAll(listOf("one", "missing")) }
        assertEquals(1, incomplete.getIfPresent("one"))
        assertEquals(9, incomplete.getIfPresent("extra"))

        val nullEntry = CacheBuilder.newBuilder<String, Int>().build(object : CacheLoader<String, Int> {
            override fun load(key: String): Int = error("single-key fallback should not run")
            @Suppress("UNCHECKED_CAST")
            override fun loadAll(keys: Iterable<String>): Map<String, Int> =
                linkedMapOf<String, Int?>("one" to 1, "bad" to null) as Map<String, Int>
        })
        assertFailsWith<CacheLoader.InvalidCacheLoadException> { nullEntry.getAll(listOf("one")) }
        assertEquals(1, nullEntry.getIfPresent("one"))
        assertNull(nullEntry.getIfPresent("bad"))
    }
}
