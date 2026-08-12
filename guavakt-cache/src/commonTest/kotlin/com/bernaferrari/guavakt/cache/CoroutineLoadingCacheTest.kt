package com.bernaferrari.guavakt.cache

import com.bernaferrari.guavakt.base.Ticker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.nanoseconds

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineLoadingCacheTest {
    @Test
    fun sameKeyRequestsShareOneOwnerScopedLoad() = runTest {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val cache = CacheBuilder.newBuilder<String, Int>()
            .recordStats()
            .buildCoroutine(backgroundScope) { key ->
                loads++
                release.await()
                key.length
            }

        val requests = List(20) { async { cache.get("shared") } }
        runCurrent()

        assertEquals(1, loads)
        assertEquals(1, cache.inFlightCount())
        release.complete(Unit)
        assertEquals(List(20) { 6 }, requests.awaitAll())

        val stats = cache.coroutineStats()
        assertEquals(1, stats.loadSuccessCount)
        assertEquals(19, stats.coalescedRequestCount)
        assertEquals(0, cache.inFlightCount())
        assertEquals(0, cache.cacheStats().loadSuccessCount, "manual backing cache does not own loads")
    }

    @Test
    fun differentKeysLoadConcurrentlyAndGetAllPreservesFirstEncounterOrder() = runTest {
        var active = 0
        var maximumActive = 0
        val release = CompletableDeferred<Unit>()
        val cache = CacheBuilder.newBuilder<String, Int>().buildCoroutine(backgroundScope) { key ->
            active++
            maximumActive = maxOf(maximumActive, active)
            try {
                release.await()
                key.length
            } finally {
                active--
            }
        }

        val result = async { cache.getAll(listOf("bbb", "a", "bbb", "cc")) }
        runCurrent()
        assertEquals(3, maximumActive)
        release.complete(Unit)

        assertEquals(listOf("bbb", "a", "cc"), result.await().keys.toList())
        assertEquals(mapOf("bbb" to 3, "a" to 1, "cc" to 2), result.await())
    }

    @Test
    fun mixedHotAndColdWorkloadUsesOneFlightPerKeyWithoutSerializingDistinctKeys() = runTest {
        val release = CompletableDeferred<Unit>()
        val startedKeys = LinkedHashSet<Int>()
        val cache = CacheBuilder.newBuilder<Int, Int>().buildCoroutine(backgroundScope) { key ->
            startedKeys += key
            release.await()
            key * key
        }
        val distinctKeys = 32
        val waitersPerKey = 8
        val requests = buildList {
            repeat(distinctKeys) { key ->
                repeat(waitersPerKey) { add(async { cache.get(key) }) }
            }
        }

        runCurrent()
        assertEquals((0 until distinctKeys).toSet(), startedKeys)
        assertEquals(distinctKeys, cache.inFlightCount())
        assertEquals(distinctKeys * (waitersPerKey - 1L), cache.coroutineStats().coalescedRequestCount)

        release.complete(Unit)
        val values = requests.awaitAll()
        assertEquals(distinctKeys * waitersPerKey, values.size)
        assertEquals(0, cache.inFlightCount())
        assertEquals(distinctKeys.toLong(), cache.coroutineStats().loadSuccessCount)
    }

    @Test
    fun cancellingOneWaiterDoesNotCancelSharedLoad() = runTest {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val cache = CacheBuilder.newBuilder<String, String>().buildCoroutine(backgroundScope) { key ->
            loads++
            release.await()
            key.uppercase()
        }

        val cancelledWaiter = async { cache.get("k") }
        val survivingWaiter = async { cache.get("k") }
        runCurrent()
        cancelledWaiter.cancelAndJoin()
        release.complete(Unit)

        assertEquals("K", survivingWaiter.await())
        assertEquals(1, loads)
        assertEquals(0, cache.coroutineStats().loadCancellationCount)
    }

    @Test
    fun ownerCancellationCancelsLoadsAndAnAlreadyCancelledOwnerDoesNotLeakFlights() = runTest {
        val ownerJob = Job()
        val ownerScope = CoroutineScope(backgroundScope.coroutineContext + ownerJob)
        val release = CompletableDeferred<Unit>()
        val cache = CacheBuilder.newBuilder<String, String>().buildCoroutine(ownerScope) {
            release.await()
            "never"
        }

        val request = async { cache.get("active") }
        runCurrent()
        ownerJob.cancel(CancellationException("owner stopped"))
        runCurrent()

        assertFailsWith<CancellationException> { request.await() }
        assertEquals(0, cache.inFlightCount())

        assertFailsWith<CancellationException> { cache.get("after-cancel") }
        assertEquals(0, cache.inFlightCount())
    }

    @Test
    fun failedLoadIsSharedThenRemovedSoNextRequestRetries() = runTest {
        var attempts = 0
        val cache = CacheBuilder.newBuilder<String, Int>().buildCoroutine(backgroundScope) {
            attempts++
            if (attempts == 1) error("first load fails")
            42
        }

        val failure = runCatching { cache.get("answer") }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals(42, cache.get("answer"))
        assertEquals(2, attempts)

        val stats = cache.coroutineStats()
        assertEquals(1, stats.loadFailureCount)
        assertEquals(1, stats.loadSuccessCount)
        assertEquals(2, stats.loadCount)
    }

    @Test
    fun refreshServesStaleValueAndOnlyReplacesItOnSuccess() = runTest {
        val release = CompletableDeferred<Unit>()
        var next = "new"
        val cache = CacheBuilder.newBuilder<String, String>().buildCoroutine(backgroundScope) {
            release.await()
            if (next == "failure") error("refresh failed")
            next
        }
        cache.put("key", "old")

        val refresh = cache.refresh("key")
        runCurrent()
        assertEquals("old", cache.get("key"), "readers keep seeing the stale value")
        release.complete(Unit)
        assertEquals("new", refresh.await())
        assertEquals("new", cache.get("key"))

        next = "failure"
        val failed = cache.refresh("key")
        runCurrent()
        assertFailsWith<IllegalStateException> { failed.await() }
        assertEquals("new", cache.get("key"), "failed refresh must not evict the good value")
        assertEquals(2, cache.coroutineStats().refreshRequestCount)
    }

    @Test
    fun cancellingRefreshWaiterDoesNotCancelTheRefresh() = runTest {
        val release = CompletableDeferred<Unit>()
        val cache = CacheBuilder.newBuilder<String, String>().buildCoroutine(backgroundScope) {
            release.await()
            "new"
        }
        cache.put("key", "old")

        val disposableWaiter = cache.refresh("key")
        runCurrent()
        disposableWaiter.cancelAndJoin()
        assertEquals(1, cache.inFlightCount())

        release.complete(Unit)
        runCurrent()
        assertEquals("new", cache.get("key"))
        assertEquals(1, cache.coroutineStats().loadSuccessCount)
    }

    @Test
    fun invalidationSupersedesEvenANonCooperativeLoader() = runTest {
        val release = CompletableDeferred<Unit>()
        val cache = CacheBuilder.newBuilder<String, String>().buildCoroutine(backgroundScope) {
            withContext(NonCancellable) { release.await() }
            "late"
        }

        val request = async { cache.get("key") }
        runCurrent()
        cache.invalidate("key")
        release.complete(Unit)
        runCurrent()

        assertFailsWith<CancellationException> { request.await() }
        assertNull(cache.getIfPresent("key"), "late load must not resurrect an invalidated entry")
        assertEquals(0, cache.inFlightCount())
    }

    @Test
    fun loadTimingUsesInjectedTicker() = runTest {
        val ticker = FakeTicker()
        val backing = CacheBuilder.newBuilder<String, Int>().build<String, Int>()
        val cache = CoroutineLoadingCache(backing, backgroundScope, SuspendingCacheLoader {
            ticker.nanos += 25
            1
        }, ticker)

        assertEquals(1, cache.get("key"))
        val stats = cache.coroutineStats()
        assertEquals(25, stats.totalLoadTimeNanos)
        assertEquals(25.0, stats.averageLoadTimeNanos)
    }

    @Test
    fun durationRefreshPolicyIsStaleWhileRevalidate() = runTest {
        val ticker = FakeTicker()
        val release = CompletableDeferred<Unit>()
        val cache = CacheBuilder.newBuilder<String, String>()
            .ticker(ticker)
            .refreshAfterWrite(10.nanoseconds)
            .buildCoroutine(backgroundScope) {
                release.await()
                "new"
            }
        cache.put("key", "old")
        ticker.nanos = 10

        assertEquals("old", cache.get("key"))
        runCurrent()
        assertEquals(1, cache.inFlightCount())
        release.complete(Unit)
        runCurrent()

        assertEquals("new", cache.get("key"))
        assertEquals(1, cache.coroutineStats().refreshRequestCount)
    }

    private class FakeTicker(var nanos: Long = 0) : Ticker() {
        override fun read(): Long = nanos
    }
}
