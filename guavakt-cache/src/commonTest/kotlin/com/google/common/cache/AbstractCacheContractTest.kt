package dev.guavakt.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AbstractCacheContractTest {
    @Test
    fun abstractCache_storageDefaultsAreUnsupported() {
        val cache = object : AbstractCache<Int, String>() {
            override fun getIfPresent(key: Int): String? = null
        }

        assertFailsWith<UnsupportedOperationException> { cache.get(1) { "one" } }
        assertFailsWith<UnsupportedOperationException> { cache.put(1, "one") }
        assertFailsWith<UnsupportedOperationException> { cache.invalidateAll() }
        assertFailsWith<UnsupportedOperationException> { cache.stats() }
        assertFailsWith<UnsupportedOperationException> { cache.asMap() }
    }

    @Test
    fun abstractLoadingCache_getAllDeduplicatesAndRefreshIsUnsupported() {
        val requested = mutableListOf<Int>()
        val cache = object : AbstractLoadingCache<Int, String>() {
            override fun getIfPresent(key: Int): String? = null
            override fun get(key: Int): String {
                requested += key
                return "value-$key"
            }
        }

        assertEquals(linkedMapOf(2 to "value-2", 1 to "value-1"), cache.getAll(listOf(2, 1, 2)))
        assertEquals(listOf(2, 1), requested)
        assertFailsWith<UnsupportedOperationException> { cache.refresh(2) }
    }
}
