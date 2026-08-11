package dev.guavakt.cache

import dev.guavakt.base.Ticker

/**
 * Guava LocalCache — LRU + size/time eviction, loading, stats, weak/soft values and weak keys.
 *
 * - **Weak/soft values:** [PlatformWeakRef] / [PlatformSoftRef] (JVM GC; strong stand-in elsewhere).
 * - **Weak keys:** key held only in [PlatformWeakRef]; lookup by identity (`===`) when key still reachable.
 *   Map index uses platform identity-hash buckets (Guava weak-key identity semantics).
 * - **Concurrency:** JVM access and loading are thread-safe and same-key loads are coalesced. This alpha
 *   uses one reentrant cache lock, so unrelated loader calls serialize; [concurrencyLevel] is a hint only.
 */
@Suppress("UNCHECKED_CAST")
class LocalCache<K, V>(
    private val maximumSize: Long,
    private val expireAfterWriteNanos: Long,
    private val ticker: Ticker,
    private val recordStats: Boolean,
    private val loader: CacheLoader<K, V>?,
    private val expireAfterAccessNanos: Long = -1L,
    private val removalListener: ((RemovalNotification<K, V>) -> Unit)? = null,
    private val keyStrength: Strength = Strength.STRONG,
    private val valueStrength: Strength = Strength.STRONG,
    private val maximumWeight: Long = -1L,
    private val weigher: ((K, V) -> Int)? = null,
    private val refreshAfterWriteNanos: Long = -1L,
    private val concurrencyLevel: Int = 4,
) : LoadingCache<K, V> {
    private val lock = Any()
    /** Retained Guava builder setting (segment hint; KMP uses single map but value is applied). */
    fun concurrencyLevel(): Int = concurrencyLevel
    private var totalWeight: Long = 0
    /** Strong-key path (default Guava STRONG keys). */
    private val strongMap = LinkedHashMap<K, Entry>()

    /** Weak-key path: identity-hash buckets → entries with weak key refs. */
    private val weakKeyBuckets = LinkedHashMap<Int, MutableList<Entry>>()

    private var hitCount = 0L
    private var missCount = 0L
    private var loadSuccessCount = 0L
    private var loadExceptionCount = 0L
    private var totalLoadTime = 0L
    private var evictionCount = 0L

    private inner class Entry(
        /** Strong key only when [keyStrength] is STRONG; otherwise null and [keyRef] holds it. */
        var strongKey: K?,
        val keyRef: PlatformWeakRef<Any>?,
        var valueHolder: ValueHolder<Any>,
        var writeTime: Long,
        var accessTime: Long,
        val identityHash: Int,
        var weight: Int = 1,
    ) {
        fun liveKey(): K? {
            if (keyStrength == Strength.STRONG) return strongKey
            @Suppress("UNCHECKED_CAST")
            return keyRef?.get() as K?
        }
    }

    private fun usesWeakKeys(): Boolean = keyStrength == Strength.WEAK

    private fun identityHash(key: K): Int = platformIdentityHashCode(key as Any)

    override fun getIfPresent(key: K): V? = monitorSync(lock) {
        getPresent(key, recordRequest = true)
    }

    /** Cache map reads affect recency but, like Guava's `asMap`, do not affect request stats. */
    private fun getPresent(key: K, recordRequest: Boolean): V? {
        cleanUp()
        val e = findEntry(key)
        if (e == null) {
            if (recordRequest && recordStats) missCount++
            return null
        }
        val value = e.valueHolder.get() as V?
        if (value == null) {
            removeEntry(e, RemovalCause.COLLECTED)
            if (recordRequest && recordStats) missCount++
            return null
        }
        if (isExpired(e)) {
            removeEntry(e, RemovalCause.EXPIRED)
            if (recordRequest && recordStats) missCount++
            return null
        }
        touchLru(e, key)
        if (recordRequest && recordStats) hitCount++
        return value
    }

    private fun findEntry(key: K): Entry? {
        if (!usesWeakKeys()) return strongMap[key]
        val bucket = weakKeyBuckets[identityHash(key)] ?: return null
        for (e in bucket) {
            val live = e.liveKey()
            if (live != null && live === key) return e
            // Also allow equals for non-identity types when weak not platform-quality
            if (live != null && !platformSupportsWeakReferences() && live == key) return e
        }
        return null
    }

    private fun touchLru(e: Entry, key: K) {
        e.accessTime = ticker.read()
        if (!usesWeakKeys()) {
            strongMap.remove(key)
            strongMap[key] = e
        }
        // weak-key: order approximated by accessTime field only
    }

    override fun get(key: K, loader: () -> V): V = monitorSync(lock) {
        getIfPresent(key) ?: loadAndPut(key, loader)
    }

    override fun get(key: K): V = monitorSync(lock) {
        val l = loader ?: throw IllegalStateException("CacheLoader not set")
        val present = getIfPresent(key)
        if (present != null) {
            maybeRefresh(key)
            val refreshed = findEntry(key)?.valueHolder?.get() as V?
            return@monitorSync refreshed ?: present
        }
        loadAndPut(key) { l.load(key) }
    }

    private fun maybeRefresh(key: K) {
        if (refreshAfterWriteNanos < 0 || loader == null) return
        val e = findEntry(key) ?: return
        val now = ticker.read()
        if (now - e.writeTime >= refreshAfterWriteNanos) {
            try {
                put(key, loader.load(key))
            } catch (_: Throwable) {
                // keep old value on refresh failure
            }
        }
    }

    private fun loadAndPut(key: K, loaderFn: () -> V): V {
        val start = ticker.read()
        return try {
            val value = loaderFn()
            put(key, value)
            if (recordStats) {
                loadSuccessCount++
                totalLoadTime += ticker.read() - start
            }
            value
        } catch (t: Throwable) {
            if (recordStats) {
                loadExceptionCount++
                totalLoadTime += ticker.read() - start
            }
            throw t
        }
    }

    override fun getAllPresent(keys: Iterable<K>): Map<K, V> = monitorSync(lock) {
        val result = LinkedHashMap<K, V>()
        for (k in keys) getIfPresent(k)?.let { result[k] = it }
        result
    }

    override fun getAll(keys: Iterable<K>): Map<K, V> = monitorSync(lock) {
        val cacheLoader = loader ?: throw IllegalStateException("CacheLoader not set")
        val result = LinkedHashMap<K, V?>()
        val keysToLoad = LinkedHashSet<K>()
        var hits = 0L

        /*
         * Guava probes the whole iterable before loading any miss. Besides preserving the
         * first-encounter order, that means duplicate cold keys are not turned into hits merely
         * because an earlier occurrence was loaded synchronously. Each distinct present key
         * records one hit; each distinct missing key is loaded once and records one miss.
         */
        for (key in keys) {
            val present = getPresent(key, recordRequest = false)
            if (key !in result) {
                if (present == null) {
                    keysToLoad += key
                    result[key] = null
                } else {
                    result[key] = present
                    hits++
                }
            }
        }
        if (recordStats) hitCount += hits

        if (keysToLoad.isNotEmpty()) {
            try {
                if (recordStats) missCount += keysToLoad.size.toLong()
                val loaded = loadAllAndPut(cacheLoader, keysToLoad)
                for (key in keysToLoad) {
                    val value = loaded[key]
                        ?: throw CacheLoader.InvalidCacheLoadException("loadAll failed to return a value for $key")
                    result[key] = value
                }
            } catch (_: CacheLoader.UnsupportedLoadingOperationException) {
                if (recordStats) missCount -= keysToLoad.size.toLong()
                for (key in keysToLoad) {
                    if (recordStats) missCount++
                    result[key] = loadAndPut(key) { cacheLoader.load(key) }
                }
            }
        }
        LinkedHashMap<K, V>(result.size).also { resolved ->
            for ((key, value) in result) {
                resolved[key] = value
                    ?: throw CacheLoader.InvalidCacheLoadException("No value was loaded for $key")
            }
        }
    }

    /** Invokes a custom bulk loader once, validates its response, and caches every valid entry. */
    private fun loadAllAndPut(cacheLoader: CacheLoader<K, V>, keys: LinkedHashSet<K>): Map<K, V> {
        val start = ticker.read()
        try {
            @Suppress("UNCHECKED_CAST")
            val loaded = cacheLoader.loadAll(keys.toSet()) as Map<K, V>?
                ?: throw CacheLoader.InvalidCacheLoadException("$cacheLoader returned null map from loadAll")
            var nullsPresent = false
            for ((key, value) in loaded) {
                if (key == null || value == null) {
                    nullsPresent = true
                } else {
                    put(key, value)
                }
            }
            if (nullsPresent) {
                throw CacheLoader.InvalidCacheLoadException("$cacheLoader returned null keys or values from loadAll")
            }
            if (recordStats) {
                loadSuccessCount++
                totalLoadTime += ticker.read() - start
            }
            return loaded
        } catch (unsupported: CacheLoader.UnsupportedLoadingOperationException) {
            throw unsupported
        } catch (failure: Throwable) {
            if (recordStats) {
                loadExceptionCount++
                totalLoadTime += ticker.read() - start
            }
            throw failure
        }
    }

    override fun put(key: K, value: V) = monitorSync(lock) {
        cleanUp()
        val now = ticker.read()
        val existing = findEntry(key)
        if (existing != null) {
            val oldVal = existing.valueHolder.get() as V?
            if (oldVal != null) notifyRemoval(existing.liveKey() ?: key, oldVal, RemovalCause.REPLACED)
            totalWeight -= existing.weight.toLong()
            existing.valueHolder.clear()
            removeEntryFromMaps(existing)
        }
        val anyVal = value as Any
        val id = identityHash(key)
        val w = weigher?.invoke(key, value) ?: 1
        require(w >= 0)
        val entry = if (usesWeakKeys()) {
            Entry(
                strongKey = null,
                keyRef = PlatformWeakRef(key as Any),
                valueHolder = ValueHolder.create(anyVal, valueStrength),
                writeTime = now,
                accessTime = now,
                identityHash = id,
                weight = w,
            )
        } else {
            Entry(
                strongKey = key,
                keyRef = null,
                valueHolder = ValueHolder.create(anyVal, valueStrength),
                writeTime = now,
                accessTime = now,
                identityHash = id,
                weight = w,
            )
        }
        if (usesWeakKeys()) {
            weakKeyBuckets.getOrPut(id) { ArrayList() }.add(entry)
        } else {
            strongMap[key] = entry
        }
        totalWeight += w.toLong()
        evictIfNeeded()
    }

    override fun putAll(m: Map<out K, V>) = monitorSync(lock) {
        for ((k, v) in m) put(k, v)
    }

    override fun invalidate(key: K) = monitorSync(lock) {
        val e = findEntry(key) ?: return@monitorSync
        val v = e.valueHolder.get() as V?
        totalWeight -= e.weight.toLong()
        e.valueHolder.clear()
        removeEntryFromMaps(e)
        if (v != null) notifyRemoval(key, v, RemovalCause.EXPLICIT)
    }

    override fun invalidateAll(keys: Iterable<K>) = monitorSync(lock) {
        for (k in keys) invalidate(k)
    }

    override fun invalidateAll() = monitorSync(lock) {
        val all = allEntries()
        strongMap.clear()
        weakKeyBuckets.clear()
        totalWeight = 0
        for (e in all) {
            val k = e.liveKey()
            val v = e.valueHolder.get() as V?
            e.valueHolder.clear()
            e.keyRef?.clear()
            if (k != null && v != null) notifyRemoval(k, v, RemovalCause.EXPLICIT)
        }
    }

    private fun allEntries(): List<Entry> = buildList {
        addAll(strongMap.values)
        for (bucket in weakKeyBuckets.values) addAll(bucket)
    }

    override fun size(): Long = monitorSync(lock) {
        cleanUp()
        allEntries().size.toLong()
    }

    override fun stats(): CacheStats = monitorSync(lock) {
        CacheStats(hitCount, missCount, loadSuccessCount, loadExceptionCount, totalLoadTime, evictionCount)
    }

    private fun liveEntryCount(): Int = monitorSync(lock) {
        cleanUp()
        allEntries().count { e ->
            e.liveKey() != null && e.valueHolder.get() != null && !isExpired(e)
        }
    }

    /** Live view: mutations go through cache put/invalidate (Guava asMap). */
    override fun asMap(): MutableMap<K, V> = object : AbstractMutableMap<K, V>() {
        override val size: Int get() = liveEntryCount()
        override fun get(key: K): V? = monitorSync(lock) { getPresent(key, recordRequest = false) }
        override fun put(key: K, value: V): V? {
            val prev = monitorSync(lock) { getPresent(key, recordRequest = false) }
            this@LocalCache.put(key, value)
            return prev
        }
        override fun remove(key: K): V? {
            val prev = monitorSync(lock) { getPresent(key, recordRequest = false) } ?: return null
            invalidate(key)
            return prev
        }
        override fun clear() = invalidateAll()
        override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
                override val size: Int get() = liveEntryCount()
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                    val snapshot = monitorSync(lock) {
                        cleanUp()
                        ArrayList<Pair<K, V>>().also { result ->
                            for (e in allEntries()) {
                                val k = e.liveKey() ?: continue
                                val v = e.valueHolder.get() as V? ?: continue
                                if (!isExpired(e)) result.add(k to v)
                            }
                        }
                    }
                    val it = snapshot.iterator()
                    return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                        private var last: Pair<K, V>? = null
                        override fun hasNext() = it.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, V> {
                            val p = it.next()
                            last = p
                            return object : MutableMap.MutableEntry<K, V> {
                                override val key: K get() = p.first
                                override val value: V get() = p.second
                                override fun setValue(newValue: V): V {
                                    val old = value
                                    this@LocalCache.put(p.first, newValue)
                                    return old
                                }
                            }
                        }
                        override fun remove() {
                            val p = last ?: throw IllegalStateException()
                            invalidate(p.first)
                            last = null
                        }
                    }
                }
                override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                    this@LocalCache.put(element.key, element.value); return true
                }
                override fun clear() = invalidateAll()
            }
    }

    override fun refresh(key: K) = monitorSync(lock) {
        loader?.let { put(key, it.load(key)) }
        Unit
    }

    override fun cleanUp() = monitorSync(lock) {
        pollClearedWeakOrSoftReferences()
        // Scrub dead weak keys and cleared values
        if (usesWeakKeys()) {
            val itBuckets = weakKeyBuckets.entries.iterator()
            while (itBuckets.hasNext()) {
                val (_, bucket) = itBuckets.next()
                val it = bucket.iterator()
                while (it.hasNext()) {
                    val e = it.next()
                    if (e.liveKey() == null) {
                        it.remove()
                        totalWeight -= e.weight.toLong()
                        evictionCount++
                        val clearedVal = e.valueHolder.get() as V?
                        e.valueHolder.clear()
                        removalListener?.invoke(
                            RemovalNotification.create(null, clearedVal, RemovalCause.COLLECTED),
                        )
                    }
                }
                if (bucket.isEmpty()) itBuckets.remove()
            }
        }
        if (valueStrength != Strength.STRONG) {
            for (e in allEntries().toList()) {
                if (e.valueHolder.get() == null && e.liveKey() != null) {
                    removeEntry(e, RemovalCause.COLLECTED)
                }
            }
        }
        if (expireAfterWriteNanos < 0 && expireAfterAccessNanos < 0) return@monitorSync
        val now = ticker.read()
        for (e in allEntries().toList()) {
            if (isExpired(e, now)) {
                removeEntry(e, RemovalCause.EXPIRED)
            }
        }
    }

    private fun isExpired(e: Entry, now: Long = ticker.read()): Boolean {
        if (expireAfterWriteNanos >= 0 && now - e.writeTime >= expireAfterWriteNanos) return true
        if (expireAfterAccessNanos >= 0 && now - e.accessTime >= expireAfterAccessNanos) return true
        return false
    }

    private fun evictIfNeeded() {
        if (maximumSize >= 0) {
            while (allEntries().size > maximumSize) {
                val e = eldestEntry() ?: break
                evictOne(e, RemovalCause.SIZE)
            }
        }
        if (maximumWeight >= 0 && weigher != null) {
            while (totalWeight > maximumWeight) {
                val e = eldestEntry() ?: break
                evictOne(e, RemovalCause.SIZE)
            }
        }
    }

    private fun evictOne(e: Entry, cause: RemovalCause) {
        val k = e.liveKey()
        val v = e.valueHolder.get() as V?
        totalWeight -= e.weight.toLong()
        removeEntryFromMaps(e)
        evictionCount++
        e.valueHolder.clear()
        e.keyRef?.clear()
        if (k != null && v != null) notifyRemoval(k, v, cause)
    }

    private fun eldestEntry(): Entry? {
        if (!usesWeakKeys()) return strongMap.values.firstOrNull()
        var best: Entry? = null
        for (e in allEntries()) {
            if (best == null || e.accessTime < best.accessTime) best = e
        }
        return best
    }

    private fun removeEntry(e: Entry, cause: RemovalCause) {
        val k = e.liveKey()
        val v = e.valueHolder.get() as V?
        totalWeight -= e.weight.toLong()
        removeEntryFromMaps(e)
        evictionCount++
        e.valueHolder.clear()
        e.keyRef?.clear()
        if (k != null && v != null) notifyRemoval(k, v, cause)
        else if (cause == RemovalCause.COLLECTED) {
            removalListener?.invoke(RemovalNotification.create(k, v, cause))
        }
    }

    private fun removeEntryFromMaps(e: Entry) {
        if (usesWeakKeys()) {
            weakKeyBuckets[e.identityHash]?.remove(e)
            if (weakKeyBuckets[e.identityHash]?.isEmpty() == true) weakKeyBuckets.remove(e.identityHash)
        } else {
            e.strongKey?.let { strongMap.remove(it) }
        }
    }

    private fun notifyRemoval(key: K, value: V, cause: RemovalCause) {
        removalListener?.invoke(RemovalNotification.create(key, value, cause))
    }

    fun usesPlatformWeakOrSoftValues(): Boolean =
        valueStrength != Strength.STRONG && platformSupportsWeakReferences()

    fun isWeakKeysEnabled(): Boolean = keyStrength == Strength.WEAK
    fun keyStrength(): Strength = keyStrength
    fun valueStrength(): Strength = valueStrength

    /** Internal age query for the coroutine facade; it neither records a request nor touches LRU. */
    internal fun isRefreshDue(key: K, thresholdNanos: Long): Boolean = monitorSync(lock) {
        cleanUp()
        val entry = findEntry(key) ?: return@monitorSync false
        if (entry.valueHolder.get() == null || isExpired(entry)) return@monitorSync false
        ticker.read() - entry.writeTime >= thresholdNanos
    }
}
