package dev.guavakt.util.concurrent

import dev.guavakt.base.Preconditions

/**
 * Guava Striped — stripes locks/semaphores by key hash (strong locks on KMP).
 */
abstract class Striped<L> {
    abstract fun get(key: Any): L
    abstract fun getAt(index: Int): L
    abstract fun size(): Int

    /** Returns one stripe per key, ordered by stripe index to make lock acquisition deadlock-safe. */
    fun bulkGet(keys: Iterable<Any>): List<L> =
        keys.map { indexFor(it, size()) }.sorted().map(::getAt)

    companion object {
        /** Eagerly creates the power-of-two stripe table from [supplier]. */
        fun <L> custom(stripes: Int, supplier: () -> L): Striped<L> {
            Preconditions.checkArgument(stripes > 0, "Stripes must be positive")
            Preconditions.checkArgument(stripes <= MAX_STRIPES, "Stripes must be <= $MAX_STRIPES")
            val size = powerOfTwo(stripes)
            val values = ArrayList<L>(size)
            repeat(size) { values += supplier() }
            return object : Striped<L>() {
                override fun get(key: Any): L = values[indexFor(key, size)]
                override fun getAt(index: Int): L = values[index]
                override fun size(): Int = size
            }
        }

        fun lock(stripes: Int): Striped<Any> {
            return custom(stripes) { Any() }
        }

        fun lazyWeakLock(stripes: Int): Striped<Any> = lock(stripes) // KMP: strong only

        fun semaphore(stripes: Int, permits: Int): Striped<SemaphoreStripe> {
            Preconditions.checkArgument(stripes > 0 && stripes <= MAX_STRIPES && permits >= 0)
            val size = powerOfTwo(stripes)
            val semaphores = Array(size) { SemaphoreStripe(permits) }
            return object : Striped<SemaphoreStripe>() {
                override fun get(key: Any): SemaphoreStripe = semaphores[indexFor(key, size)]
                override fun getAt(index: Int): SemaphoreStripe = semaphores[index]
                override fun size(): Int = size
            }
        }

        private fun powerOfTwo(stripes: Int): Int {
            var n = 1
            while (n < stripes) n = n shl 1
            return n
        }

        private fun indexFor(key: Any, size: Int): Int {
            var hash = key.hashCode()
            hash = hash xor (hash ushr 20) xor (hash ushr 12)
            hash = hash xor (hash ushr 7) xor (hash ushr 4)
            return hash and (size - 1)
        }

        private const val MAX_STRIPES: Int = 1 shl 30
    }
}

/** Simple permit counter (not OS semaphore). */
class SemaphoreStripe(private val maxPermits: Int) {
    private var available = maxPermits
    fun tryAcquire(): Boolean = monitorSync(this) {
        if (available > 0) { available--; true } else false
    }
    fun release() = monitorSync(this) {
        if (available < maxPermits) available++
    }
    fun availablePermits(): Int = monitorSync(this) { available }
}
