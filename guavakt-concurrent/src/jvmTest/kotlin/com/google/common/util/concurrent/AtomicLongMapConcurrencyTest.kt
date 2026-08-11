package dev.guavakt.util.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtomicLongMapConcurrencyTest {
    @Test
    fun contendedUpdatesAreAtomicAndInvokeEachCallbackOnce() {
        val map = AtomicLongMap.create<String>()
        val callbackCalls = AtomicInteger()
        val workers = 6
        val iterations = 5_000
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        val done = CountDownLatch(workers)
        val executor = Executors.newFixedThreadPool(workers)

        try {
            repeat(workers) {
                executor.execute {
                    ready.countDown()
                    start.await()
                    repeat(iterations) {
                        map.updateAndGet("shared") { current ->
                            callbackCalls.incrementAndGet()
                            current + 1L
                        }
                    }
                    done.countDown()
                }
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        val expected = workers.toLong() * iterations
        assertEquals(expected, map.get("shared"))
        assertEquals(expected.toInt(), callbackCalls.get())
        assertEquals(mapOf("shared" to expected), map.asMap())
    }
}
