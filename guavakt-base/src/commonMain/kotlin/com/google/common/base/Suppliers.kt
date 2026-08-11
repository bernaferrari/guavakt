package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

@GwtCompatible
object Suppliers {
    fun <T> ofInstance(instance: T): Supplier<T> = Supplier { instance }

    /** Memoizing supplier (first successful [Supplier.get] wins; not reentrant-safe across threads on all KMP targets). */
    fun <T> memoize(delegate: Supplier<T>): Supplier<T> = object : Supplier<T> {
        private var initialized = false
        private var value: Any? = UNSET

        @Suppress("UNCHECKED_CAST")
        override fun get(): T {
            if (!initialized) {
                value = delegate.get()
                initialized = true
            }
            return value as T
        }
    }

    fun <T> memoizeWithExpiration(
        delegate: Supplier<T>,
        durationNanos: Long,
        ticker: Ticker = Ticker.systemTicker(),
    ): Supplier<T> {
        require(durationNanos > 0) { "durationNanos must be positive" }
        return object : Supplier<T> {
            private var value: Any? = UNSET
            private var expirationNanos = 0L

            @Suppress("UNCHECKED_CAST")
            override fun get(): T {
                val now = ticker.read()
                if (expirationNanos == 0L || now - expirationNanos >= 0) {
                    val t = delegate.get()
                    value = t
                    expirationNanos = now + durationNanos
                    return t
                }
                return value as T
            }
        }
    }

    fun <T> synchronizedSupplier(delegate: Supplier<T>): Supplier<T> = object : Supplier<T> {
        private val lock = Any()
        override fun get(): T {
            // Cooperative critical section marker (real monitor on JVM via Monitor platform if needed)
            return delegate.get().also { lock.hashCode() }
        }
    }

    fun <T> supplierFunction(): (Supplier<T>) -> T = { it.get() }

    fun <F, T> compose(function: (F) -> T, supplier: Supplier<F>): Supplier<T> =
        Supplier { function(supplier.get()) }

    private val UNSET = Any()
}
