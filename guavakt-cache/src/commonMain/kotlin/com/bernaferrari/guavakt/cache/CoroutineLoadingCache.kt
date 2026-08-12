package com.bernaferrari.guavakt.cache

import com.bernaferrari.guavakt.base.Ticker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A coroutine-native loading facade over a synchronous [Cache].
 *
 * The supplied [scope] owns every load and refresh. A caller only awaits shared work, so cancelling
 * one caller does not cancel a load that other callers need. Cancelling [scope] cancels all work.
 * There is never more than one active load per key, while different keys may load concurrently.
 *
 * [refresh] uses stale-while-revalidate semantics: the old value remains readable until a successful
 * replacement is installed, and a failed refresh leaves it untouched. [put], [invalidate], and
 * [invalidateAll] supersede active loads so a late result cannot resurrect or overwrite an entry.
 *
 * The synchronous cache remains available as [synchronousCache] for inspection and Guava-shaped
 * operations. Mutating it directly bypasses the in-flight coordination guarantees above.
 */
class CoroutineLoadingCache<K, V> internal constructor(
    val synchronousCache: Cache<K, V>,
    private val scope: CoroutineScope,
    private val loader: SuspendingCacheLoader<K, V>,
    private val ticker: Ticker,
    private val refreshAfterWriteNanos: Long,
) {
    constructor(
        synchronousCache: Cache<K, V>,
        scope: CoroutineScope,
        loader: SuspendingCacheLoader<K, V>,
        ticker: Ticker = Ticker.systemTicker(),
    ) : this(synchronousCache, scope, loader, ticker, -1L)

    private class Flight<V> {
        lateinit var deferred: Deferred<Result<V>>
    }

    private val stateMutex = Mutex()
    private val inFlight = mutableMapOf<K, Flight<V>>()
    private var loadSuccessCount = 0L
    private var loadFailureCount = 0L
    private var loadCancellationCount = 0L
    private var coalescedRequestCount = 0L
    private var refreshRequestCount = 0L
    private var totalLoadTimeNanos = 0L

    /** Returns the cached value or awaits the one shared load for [key]. */
    suspend fun get(key: K): V =
        acquire(key, useCachedValue = true, isRefresh = false).await().getOrThrow()

    /**
     * Returns values for the distinct [keys], preserving their first-encounter order.
     * Loads for different keys are started concurrently.
     */
    suspend fun getAll(keys: Iterable<K>): Map<K, V> = coroutineScope {
        val distinctKeys = LinkedHashSet<K>()
        distinctKeys.addAll(keys)
        val entries = distinctKeys.map { key -> async { key to get(key) } }.awaitAll()
        buildMap(entries.size) {
            entries.forEach { (key, value) -> put(key, value) }
        }
    }

    /**
     * Starts or joins a refresh and returns a waiter owned by this cache's [scope].
     * Cancelling the returned waiter does not cancel the shared per-key load.
     */
    fun refresh(key: K): Deferred<V> = ownerWaiter {
        acquire(key, useCachedValue = false, isRefresh = true).await().getOrThrow()
    }

    fun getIfPresent(key: K): V? = synchronousCache.getIfPresent(key)

    fun getAllPresent(keys: Iterable<K>): Map<K, V> = synchronousCache.getAllPresent(keys)

    /** Installs [value] and prevents an older active load from overwriting it. */
    suspend fun put(key: K, value: V) {
        val superseded = stateMutex.withLock {
            val flight = inFlight.remove(key)
            synchronousCache.put(key, value)
            flight?.deferred
        }
        superseded?.cancel(CancellationException("cache value was replaced"))
    }

    suspend fun putAll(values: Map<out K, V>) {
        val superseded = stateMutex.withLock {
            val flights = values.keys.mapNotNull { key -> inFlight.remove(key)?.deferred }
            synchronousCache.putAll(values)
            flights
        }
        superseded.forEach { it.cancel(CancellationException("cache value was replaced")) }
    }

    /** Invalidates [key] and prevents an older active load from restoring it. */
    suspend fun invalidate(key: K) {
        val superseded = stateMutex.withLock {
            val flight = inFlight.remove(key)
            synchronousCache.invalidate(key)
            flight?.deferred
        }
        superseded?.cancel(CancellationException("cache entry was invalidated"))
    }

    suspend fun invalidateAll(keys: Iterable<K>) {
        val keySnapshot = keys.toList()
        val superseded = stateMutex.withLock {
            val flights = keySnapshot.mapNotNull { key -> inFlight.remove(key)?.deferred }
            synchronousCache.invalidateAll(keySnapshot)
            flights
        }
        superseded.forEach { it.cancel(CancellationException("cache entry was invalidated")) }
    }

    /** Invalidates all entries and cancels every active load. */
    suspend fun invalidateAll() {
        val superseded = stateMutex.withLock {
            val flights = inFlight.values.map { it.deferred }
            inFlight.clear()
            synchronousCache.invalidateAll()
            flights
        }
        superseded.forEach { it.cancel(CancellationException("cache was invalidated")) }
    }

    fun size(): Long = synchronousCache.size()

    fun asMap(): Map<K, V> = synchronousCache.asMap()

    fun cleanUp() = synchronousCache.cleanUp()

    /** Guava-shaped hit/miss, eviction, and expiration statistics from the backing cache. */
    fun cacheStats(): CacheStats = synchronousCache.stats()

    /** Coroutine load, coalescing, refresh, cancellation, and timing statistics. */
    suspend fun coroutineStats(): CoroutineCacheStats = stateMutex.withLock {
        CoroutineCacheStats(
            loadSuccessCount = loadSuccessCount,
            loadFailureCount = loadFailureCount,
            loadCancellationCount = loadCancellationCount,
            coalescedRequestCount = coalescedRequestCount,
            refreshRequestCount = refreshRequestCount,
            totalLoadTimeNanos = totalLoadTimeNanos,
        )
    }

    suspend fun inFlightCount(): Int = stateMutex.withLock {
        inFlight.entries.removeAll { it.value.deferred.isCompleted }
        inFlight.size
    }

    private suspend fun acquire(
        key: K,
        useCachedValue: Boolean,
        isRefresh: Boolean,
    ): Deferred<Result<V>> {
        var created: Flight<V>? = null
        val result = stateMutex.withLock {
            if (isRefresh) refreshRequestCount++

            if (useCachedValue) {
                synchronousCache.getIfPresent(key)?.let {
                    if (automaticRefreshDue(key)) {
                        refreshRequestCount++
                        if (activeFlight(key) == null) created = createFlight(key)
                    }
                    return@withLock CompletableDeferred(Result.success(it))
                }
            }

            activeFlight(key)?.let {
                coalescedRequestCount++
                return@withLock it.deferred
            }

            createFlight(key).also { created = it }.deferred
        }
        // Starting outside the mutex also lets an already-cancelled owner complete and prune cleanly.
        created?.deferred?.start()
        return result
    }

    private fun activeFlight(key: K): Flight<V>? {
        val flight = inFlight[key] ?: return null
        if (flight.deferred.isCompleted) {
            inFlight.remove(key)
            return null
        }
        return flight
    }

    private fun createFlight(key: K): Flight<V> {
        val flight = Flight<V>()
        flight.deferred = scope.async(start = CoroutineStart.LAZY) {
            executeLoad(key, flight)
        }
        inFlight[key] = flight
        flight.deferred.invokeOnCompletion {
            // Normal completion is removed by executeLoad. This covers cancellation before start.
            if (stateMutex.tryLock()) {
                try {
                    if (inFlight[key] === flight) inFlight.remove(key)
                } finally {
                    stateMutex.unlock()
                }
            }
        }
        return flight
    }

    @Suppress("UNCHECKED_CAST")
    private fun automaticRefreshDue(key: K): Boolean {
        if (refreshAfterWriteNanos < 0) return false
        val local = synchronousCache as? LocalCache<K, V> ?: return false
        return local.isRefreshDue(key, refreshAfterWriteNanos)
    }

    private suspend fun executeLoad(key: K, flight: Flight<V>): Result<V> {
        val startedAt = ticker.read()
        try {
            val value = loader.load(key)
            stateMutex.withLock {
                // A coordinated put/invalidation may have superseded this result while it loaded.
                if (inFlight[key] === flight) synchronousCache.put(key, value)
                loadSuccessCount++
                addElapsedTime(startedAt)
            }
            return Result.success(value)
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                stateMutex.withLock {
                    loadCancellationCount++
                    addElapsedTime(startedAt)
                }
            }
            return Result.failure(cancelled)
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                stateMutex.withLock {
                    loadFailureCount++
                    addElapsedTime(startedAt)
                }
            }
            return Result.failure(failure)
        } finally {
            withContext(NonCancellable) {
                stateMutex.withLock {
                    if (inFlight[key] === flight) inFlight.remove(key)
                }
            }
        }
    }

    private fun addElapsedTime(startedAt: Long) {
        val elapsed = ticker.read() - startedAt
        if (elapsed > 0) totalLoadTimeNanos += elapsed
    }

    /**
     * Creates a cancellable observer without making loader failure a failure of the owner scope.
     * The coordinator is owner-scoped; the result itself is deliberately not a parent of the load.
     */
    private fun ownerWaiter(block: suspend () -> V): Deferred<V> {
        val waiter = CompletableDeferred<V>()
        val coordinator: Job = scope.launch {
            try {
                waiter.complete(block())
            } catch (cancelled: CancellationException) {
                waiter.cancel(cancelled)
            } catch (failure: Throwable) {
                waiter.completeExceptionally(failure)
            }
        }
        coordinator.invokeOnCompletion { cause ->
            if (cause != null && !waiter.isCompleted) {
                if (cause is CancellationException) waiter.cancel(cause)
                else waiter.completeExceptionally(cause)
            }
        }
        waiter.invokeOnCompletion {
            if (waiter.isCancelled) coordinator.cancel()
        }
        return waiter
    }
}
