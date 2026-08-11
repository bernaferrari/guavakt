package dev.guavakt.base.internal

import java.lang.ref.Reference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformFinalizerReferencesJvmTest {
    @Test
    fun enqueuedReferenceDispatchesItsCallbackFromTheDaemon() {
        val queue = PlatformFinalizerQueue()
        val callback = CountDownLatch(1)
        val handle = queue.weakReference(Any()) { callback.countDown() }

        try {
            val field = handle.javaClass.getDeclaredField("delegate")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val reference = field.get(handle) as Reference<Any>
            assertTrue(reference.enqueue())
            assertTrue(callback.await(2, TimeUnit.SECONDS), "queued callback was not dispatched")
            assertEquals(1, queue.cleanupCount())
        } finally {
            queue.close()
        }
    }
}
