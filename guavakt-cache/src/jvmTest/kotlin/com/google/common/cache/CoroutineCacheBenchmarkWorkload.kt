package dev.guavakt.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger

/** JVM-only deterministic workloads called by the JMH harness. */
class CoroutineCacheBenchmarkWorkload {
    private val owner = SupervisorJob()
    private val scope = CoroutineScope(owner + Dispatchers.Default)
    private val loadStarts = AtomicInteger()

    @Volatile private var expectedLoads = 0
    @Volatile private var releaseLoads: CompletableDeferred<Unit>? = null
    @Volatile private var allLoadsStarted = CompletableDeferred<Unit>()

    private val cache = CacheBuilder.newBuilder<Int, Int>()
        .maximumSize(256)
        .buildCoroutine(scope) { key ->
            val release = releaseLoads
            if (release != null) {
                if (loadStarts.incrementAndGet() == expectedLoads) allLoadsStarted.complete(Unit)
                release.await()
            }
            key
        }

    fun prepareHotHits() {
        reset(expectedLoads = 0)
    }

    fun prepareSameKeyMisses() {
        // The barrier must observe the one shared loader, not every waiter that joins it.
        reset(expectedLoads = 1)
    }

    fun prepareDistinctKeyMisses() {
        reset(expectedLoads = DISTINCT_KEYS_PER_BATCH)
    }

    fun prepareEvictingWrites() {
        reset(expectedLoads = 0)
    }

    fun hotHitBatch(): Int = runBlocking {
        var total = 0
        repeat(HOT_HITS_PER_BATCH) { total += cache.get(HOT_KEY) }
        total
    }

    fun sameKeyMissBatch(): Int = runBlocking {
        coroutineScope {
            val requests = List(WAITERS_PER_BATCH) {
                async(start = CoroutineStart.UNDISPATCHED) { cache.get(SHARED_MISS_KEY) }
            }
            allLoadsStarted.await()
            releaseLoads!!.complete(Unit)
            requests.awaitAll().sum()
        }
    }

    fun distinctKeyMissBatch(): Int = runBlocking {
        coroutineScope {
            val requests = List(DISTINCT_KEYS_PER_BATCH) { key ->
                async(start = CoroutineStart.UNDISPATCHED) { cache.get(key + FIRST_DISTINCT_MISS_KEY) }
            }
            allLoadsStarted.await()
            releaseLoads!!.complete(Unit)
            requests.awaitAll().sum()
        }
    }

    /** Measures bounded-cache maintenance under writes, including LRU eviction bookkeeping. */
    fun evictingWriteBatch(): Int = runBlocking {
        repeat(EVICTING_WRITES_PER_BATCH) { key -> cache.put(key + FIRST_EVICTING_KEY, key) }
        cache.size().toInt()
    }

    fun close() = scope.cancel()

    private fun reset(expectedLoads: Int) = runBlocking {
        cache.invalidateAll()
        cache.put(HOT_KEY, HOT_VALUE)
        this@CoroutineCacheBenchmarkWorkload.expectedLoads = expectedLoads
        loadStarts.set(0)
        allLoadsStarted = CompletableDeferred()
        releaseLoads = if (expectedLoads == 0) null else CompletableDeferred()
    }

    private companion object {
        const val HOT_KEY = -1
        const val HOT_VALUE = 1
        const val SHARED_MISS_KEY = 1
        const val FIRST_DISTINCT_MISS_KEY = 1_000
        const val HOT_HITS_PER_BATCH = 1_024
        const val WAITERS_PER_BATCH = 64
        const val DISTINCT_KEYS_PER_BATCH = 64
        const val FIRST_EVICTING_KEY = 10_000
        const val EVICTING_WRITES_PER_BATCH = 512
    }
}
