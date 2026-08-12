package com.bernaferrari.guavakt.cache

import kotlin.test.Test
import kotlin.test.assertEquals

class RemovalListenerContractTest {
    @Test
    fun typedListenerReceivesReplacementAfterEntryIsDetached() {
        val received = mutableListOf<RemovalNotification<String, Int>>()
        val cache = CacheBuilder.newBuilder<String, Int>()
            .removalListener(RemovalListener { received += it })
            .build<String, Int>()

        cache.put("key", 1)
        cache.put("key", 2)

        assertEquals(1, received.size)
        assertEquals("key", received.single().getKey())
        assertEquals(1, received.single().getValue())
        assertEquals(RemovalCause.REPLACED, received.single().cause)
        assertEquals(2, cache.getIfPresent("key"))
    }

}
