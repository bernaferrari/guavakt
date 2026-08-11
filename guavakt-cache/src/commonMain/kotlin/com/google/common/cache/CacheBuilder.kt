package dev.guavakt.cache

import dev.guavakt.base.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * Guava CacheBuilder — builds [LocalCache]. Each policy may be configured once; conflicting or
 * repeated settings fail immediately, matching Guava's migration-safe builder contract.
 */
class CacheBuilder<K, V> private constructor() {
    private var maximumSize: Long = -1
    private var maximumWeight: Long = -1
    private var weigher: ((K, V) -> Int)? = null
    private var expireAfterWriteNanos: Long = -1
    private var expireAfterAccessNanos: Long = -1
    private var refreshAfterWriteNanos: Long = -1
    private var concurrencyLevel: Int = 4
    private var ticker: Ticker = Ticker.systemTicker()
    private var recordStats = false
    private var keyStrength: Strength = Strength.STRONG
    private var valueStrength: Strength = Strength.STRONG
    private var removalListener: ((RemovalNotification<K, V>) -> Unit)? = null
    private var maximumSizeSet = false
    private var maximumWeightSet = false
    private var expireAfterWriteSet = false
    private var expireAfterAccessSet = false
    private var refreshAfterWriteSet = false
    private var concurrencyLevelSet = false
    private var tickerSet = false
    private var keyStrengthSet = false
    private var valueStrengthSet = false
    private var removalListenerSet = false

    fun maximumSize(size: Long): CacheBuilder<K, V> = apply {
        check(!maximumSizeSet && !maximumWeightSet && weigher == null)
        require(size >= 0); maximumSize = size; maximumSizeSet = true
    }
    fun maximumWeight(weight: Long): CacheBuilder<K, V> = apply {
        check(!maximumWeightSet && !maximumSizeSet)
        require(weight >= 0); maximumWeight = weight; maximumWeightSet = true
    }
    fun weigher(w: (K, V) -> Int): CacheBuilder<K, V> = apply {
        check(weigher == null && !maximumSizeSet)
        weigher = w
    }
    /** Guava-shaped typed weigher overload; Kotlin lambdas may use the function overload above. */
    fun weigher(weigher: Weigher<K, V>): CacheBuilder<K, V> =
        weigher { key, value -> weigher.weigh(key, value) }
    fun expireAfterWriteMillis(millis: Long): CacheBuilder<K, V> = apply {
        check(!expireAfterWriteSet)
        require(millis >= 0); expireAfterWriteNanos = millisToNanosSaturated(millis); expireAfterWriteSet = true
    }
    fun expireAfterWrite(duration: Duration): CacheBuilder<K, V> = apply {
        check(!expireAfterWriteSet)
        require(!duration.isNegative())
        expireAfterWriteNanos = duration.inWholeNanoseconds; expireAfterWriteSet = true
    }
    fun expireAfterAccessMillis(millis: Long): CacheBuilder<K, V> = apply {
        check(!expireAfterAccessSet)
        require(millis >= 0); expireAfterAccessNanos = millisToNanosSaturated(millis); expireAfterAccessSet = true
    }
    fun expireAfterAccess(duration: Duration): CacheBuilder<K, V> = apply {
        check(!expireAfterAccessSet)
        require(!duration.isNegative())
        expireAfterAccessNanos = duration.inWholeNanoseconds; expireAfterAccessSet = true
    }
    fun refreshAfterWriteMillis(millis: Long): CacheBuilder<K, V> = apply {
        check(!refreshAfterWriteSet)
        require(millis > 0); refreshAfterWriteNanos = millisToNanosSaturated(millis); refreshAfterWriteSet = true
    }
    fun refreshAfterWrite(duration: Duration): CacheBuilder<K, V> = apply {
        check(!refreshAfterWriteSet)
        require(duration.isPositive())
        refreshAfterWriteNanos = duration.inWholeNanoseconds; refreshAfterWriteSet = true
    }
    fun concurrencyLevel(level: Int): CacheBuilder<K, V> = apply {
        check(!concurrencyLevelSet)
        require(level > 0); concurrencyLevel = level; concurrencyLevelSet = true
    }
    fun ticker(ticker: Ticker): CacheBuilder<K, V> = apply {
        check(!tickerSet)
        this.ticker = ticker; tickerSet = true
    }
    fun recordStats(): CacheBuilder<K, V> = apply { recordStats = true }
    fun weakKeys(): CacheBuilder<K, V> = apply {
        check(!keyStrengthSet); keyStrength = Strength.WEAK; keyStrengthSet = true
    }
    fun weakValues(): CacheBuilder<K, V> = apply {
        check(!valueStrengthSet); valueStrength = Strength.WEAK; valueStrengthSet = true
    }
    fun softValues(): CacheBuilder<K, V> = apply {
        check(!valueStrengthSet); valueStrength = Strength.SOFT; valueStrengthSet = true
    }
    fun removalListener(listener: (RemovalNotification<K, V>) -> Unit): CacheBuilder<K, V> =
        apply { check(!removalListenerSet); removalListener = listener; removalListenerSet = true }

    /** Guava-shaped typed listener overload; Kotlin lambdas may use the function overload above. */
    fun removalListener(listener: RemovalListener<K, V>): CacheBuilder<K, V> =
        removalListener { notification -> listener.onRemoval(notification) }

    fun concurrencyLevelOrDefault(): Int = concurrencyLevel

    private fun validateBuilderState() {
        // Guava: maximumWeight requires weigher; weigher requires maximumWeight; not both size+weight
        if (maximumWeight >= 0) {
            require(weigher != null) { "maximumWeight requires weigher" }
        }
        if (weigher != null) {
            require(maximumWeight >= 0) { "weigher requires maximumWeight" }
        }
        if (maximumSize >= 0 && maximumWeight >= 0) {
            error("maximumSize and maximumWeight cannot both be set")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <K1 : K, V1 : V> build(): Cache<K1, V1> {
        validateBuilderState()
        return LocalCache(
            effectiveMaximumSize(), expireAfterWriteNanos, ticker, recordStats, null,
            expireAfterAccessNanos, removalListener as ((RemovalNotification<K1, V1>) -> Unit)?,
            keyStrength, valueStrength, maximumWeight,
            weigher as ((K1, V1) -> Int)?, refreshAfterWriteNanos, concurrencyLevel,
        ) as Cache<K1, V1>
    }

    @Suppress("UNCHECKED_CAST")
    fun <K1 : K, V1 : V> build(loader: CacheLoader<K1, V1>): LoadingCache<K1, V1> {
        validateBuilderState()
        return LocalCache(
            effectiveMaximumSize(), expireAfterWriteNanos, ticker, recordStats, loader,
            expireAfterAccessNanos, removalListener as ((RemovalNotification<K1, V1>) -> Unit)?,
            keyStrength, valueStrength, maximumWeight,
            weigher as ((K1, V1) -> Int)?, refreshAfterWriteNanos, concurrencyLevel,
        ) as LoadingCache<K1, V1>
    }

    /**
     * Builds a coroutine-native loading cache whose asynchronous work belongs to [scope].
     *
     * Unlike [build] with a [CacheLoader], suspending loads for different keys may execute
     * concurrently. Requests for the same missing key share one load. Cancelling an individual
     * waiter does not cancel that shared work; cancelling [scope] does.
     *
     * When `refreshAfterWrite` is configured, a read of an old entry starts an owner-scoped refresh
     * and immediately returns the stale value. Refresh failure remains observable in coroutine stats
     * and leaves that value intact. [CoroutineLoadingCache.refresh] can also be used explicitly.
     */
    fun buildCoroutine(
        scope: CoroutineScope,
        loader: SuspendingCacheLoader<K, V>,
    ): CoroutineLoadingCache<K, V> {
        validateBuilderState()
        val cache = LocalCache(
            effectiveMaximumSize(), expireAfterWriteNanos, ticker, recordStats, null,
            expireAfterAccessNanos, removalListener,
            keyStrength, valueStrength, maximumWeight,
            weigher, -1, concurrencyLevel,
        ) as Cache<K, V>
        return CoroutineLoadingCache(cache, scope, loader, ticker, refreshAfterWriteNanos)
    }

    /** Matches `TimeUnit.MILLISECONDS.toNanos`: positive values saturate rather than wrap. */
    private fun millisToNanosSaturated(millis: Long): Long =
        if (millis > Long.MAX_VALUE / NANOS_PER_MILLI) Long.MAX_VALUE else millis * NANOS_PER_MILLI

    /** Guava defines zero write/access expiry as a zero-size cache, including `SIZE` removal cause. */
    private fun effectiveMaximumSize(): Long =
        if (expireAfterWriteNanos == 0L || expireAfterAccessNanos == 0L) 0L else maximumSize

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000L
        fun <K, V> newBuilder(): CacheBuilder<K, V> = CacheBuilder()
    }
}
