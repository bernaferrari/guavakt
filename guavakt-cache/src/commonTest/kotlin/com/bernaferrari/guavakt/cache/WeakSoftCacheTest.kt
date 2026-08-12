package com.bernaferrari.guavakt.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeakSoftCacheTest {
    @Test
    fun weakValues_api_and_strong_lookup() {
        val cache = CacheBuilder.newBuilder<String, String>()
            .weakValues()
            .maximumSize(100)
            .build<String, String>()
        cache.put("a", "cat")
        assertEquals("cat", cache.getIfPresent("a"))
        assertTrue(cache.size() >= 0)
    }

    @Test
    fun softValues_api() {
        val cache = CacheBuilder.newBuilder<String, Int>()
            .softValues()
            .build<String, Int>()
        cache.put("k", 1)
        assertEquals(1, cache.getIfPresent("k"))
    }

    @Test
    fun weakKeys_api_lookup_by_same_instance() {
        val cache = CacheBuilder.newBuilder<Any, String>()
            .weakKeys()
            .build<Any, String>()
        val key = Any()
        cache.put(key, "v")
        assertEquals("v", cache.getIfPresent(key))
        @Suppress("UNCHECKED_CAST")
        val lc = cache as LocalCache<Any, String>
        assertTrue(lc.isWeakKeysEnabled())
    }

    @Test
    fun platform_support_flag_consistent_with_uses() {
        @Suppress("UNCHECKED_CAST")
        val cache = CacheBuilder.newBuilder<String, String>().weakValues().build<String, String>() as LocalCache<String, String>
        assertEquals(platformSupportsWeakReferences(), cache.usesPlatformWeakOrSoftValues())
        assertEquals(Strength.WEAK, cache.valueStrength())
    }
}
