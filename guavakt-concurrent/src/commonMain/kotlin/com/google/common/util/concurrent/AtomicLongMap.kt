package dev.guavakt.util.concurrent

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A map of non-null keys to `Long` counters that can be updated atomically.
 *
 * An absent key has an implicit value of zero, but a key explicitly mapped to zero remains present.
 * Consequently [get] treats those states alike while [containsKey], [size], [isEmpty], [asMap], and
 * [toString] distinguish them. Zero-valued entries remain until explicitly removed.
 *
 * Reads use striped immutable atomic snapshots and never block. Updates within each stripe are
 * serialized with a common Kotlin atomic, so read-modify-write operations are atomic on JVM and
 * thread-capable Native targets and have the corresponding single-event-loop guarantee on JS and
 * Wasm. Unrelated stripes can update concurrently, and each write copies only its stripe. Update
 * callbacks are invoked exactly once. As with `ConcurrentHashMap.compute`, callbacks must be short
 * and must not recursively update this same map.
 *
 * [asMap] is a cached, live, read-only view. Iterators observe a stable snapshot taken when the
 * iterator is created.
 */
@OptIn(ExperimentalAtomicApi::class)
class AtomicLongMap<K : Any> private constructor(initialValues: Map<K, Long>) {
    private val stripes: List<Stripe<K>> = run {
        val grouped = List(STRIPE_COUNT) { LinkedHashMap<K, Long>() }
        initialValues.forEach { (key, value) -> grouped[stripeIndex(key)][key] = value }
        List(STRIPE_COUNT) { index -> Stripe(grouped[index].toMap()) }
    }
    private val entrySetView: MutableSet<MutableMap.MutableEntry<K, Long>> = LiveEntrySet()
    private val keySetView: MutableSet<K> = LiveKeySet()
    private val valuesView: MutableCollection<Long> = LiveValues()
    private val mapView: Map<K, Long> = LiveMapView()

    /** Returns the mapped value, or zero when [key] is absent. */
    fun get(key: K): Long = stripeFor(key).snapshot()[key] ?: 0L

    fun incrementAndGet(key: K): Long = addAndGet(key, 1L)

    fun decrementAndGet(key: K): Long = addAndGet(key, -1L)

    fun addAndGet(key: K, delta: Long): Long = accumulateAndGet(key, delta, Long::plus)

    fun getAndIncrement(key: K): Long = getAndAdd(key, 1L)

    fun getAndDecrement(key: K): Long = getAndAdd(key, -1L)

    fun getAndAdd(key: K, delta: Long): Long = getAndAccumulate(key, delta, Long::plus)

    /** Applies [updaterFunction] to the current value, or zero when absent, and returns the result. */
    fun updateAndGet(key: K, updaterFunction: (Long) -> Long): Long = mutate(key) { current ->
        val updated = updaterFunction(current[key] ?: 0L)
        Mutation(current.withMapping(key, updated), updated)
    }

    /** Applies [updaterFunction] atomically and returns the value that it replaced. */
    fun getAndUpdate(key: K, updaterFunction: (Long) -> Long): Long = mutate(key) { current ->
        val oldValue = current[key] ?: 0L
        Mutation(current.withMapping(key, updaterFunction(oldValue)), oldValue)
    }

    /** Combines the current value (or zero when absent) with [x], returning the new value. */
    fun accumulateAndGet(key: K, x: Long, accumulatorFunction: (Long, Long) -> Long): Long =
        updateAndGet(key) { accumulatorFunction(it, x) }

    /** Combines the current value with [x] atomically, returning the replaced value. */
    fun getAndAccumulate(key: K, x: Long, accumulatorFunction: (Long, Long) -> Long): Long =
        getAndUpdate(key) { accumulatorFunction(it, x) }

    /** Associates [newValue] with [key] and returns the previous value, or zero when absent. */
    fun put(key: K, newValue: Long): Long = getAndUpdate(key) { newValue }

    /** Copies [values] into this map. Each individual mapping is installed atomically. */
    fun putAll(values: Map<out K, Long>) {
        values.forEach { (key, value) -> put(key, value) }
    }

    /** Removes [key] and returns its previous value, or zero when absent. */
    fun remove(key: K): Long = mutate(key) { current ->
        if (!current.containsKey(key)) {
            Mutation(current, 0L)
        } else {
            Mutation(current.withoutMapping(key), current.getValue(key))
        }
    }

    /** Removes [key] only when it is explicitly mapped to zero. */
    fun removeIfZero(key: K): Boolean = remove(key, 0L)

    /** Removes every explicitly zero-valued mapping. */
    fun removeAllZeros() {
        stripes.forEach { stripe ->
            stripe.mutate { current ->
                val filtered = LinkedHashMap<K, Long>()
                current.forEach { (key, value) -> if (value != 0L) filtered[key] = value }
                Mutation(filtered, Unit)
            }
        }
    }

    /** Returns a non-atomic sum that may include some concurrent operations and exclude others. */
    fun sum(): Long = stripes.fold(0L) { total, stripe ->
        total + stripe.snapshot().values.fold(0L, Long::plus)
    }

    /** Returns a cached, live, read-only view of this map. */
    fun asMap(): Map<K, Long> = mapView

    fun containsKey(key: K): Boolean = stripeFor(key).snapshot().containsKey(key)

    fun size(): Int {
        var total = 0L
        for (stripe in stripes) {
            total += stripe.snapshot().size
            if (total >= Int.MAX_VALUE) return Int.MAX_VALUE
        }
        return total.toInt()
    }

    fun isEmpty(): Boolean = stripes.all { it.snapshot().isEmpty() }

    /** Clears every stripe. Concurrent writes may be visible during or after this operation. */
    fun clear() {
        stripes.forEach { stripe -> stripe.mutate { Mutation(emptyMap(), Unit) } }
    }

    override fun toString(): String = snapshot().toString()

    /** Guava package-private operation retained internally for parity tests and implementation use. */
    internal fun remove(key: K, value: Long): Boolean = mutate(key) { current ->
        if (current.containsKey(key) && current[key] == value) {
            Mutation(current.withoutMapping(key), true)
        } else {
            Mutation(current, false)
        }
    }

    /** Guava package-private operation: absent and explicitly-zero both count as no value. */
    internal fun putIfAbsent(key: K, newValue: Long): Long = mutate(key) { current ->
        val oldValue = current[key]
        if (oldValue == null || oldValue == 0L) {
            Mutation(current.withMapping(key, newValue), 0L)
        } else {
            Mutation(current, oldValue)
        }
    }

    /** Guava package-private operation, including its special absent-as-zero rule. */
    internal fun replace(key: K, expectedOldValue: Long, newValue: Long): Boolean = mutate(key) { current ->
        val matches =
            if (expectedOldValue == 0L) current[key] == null || current[key] == 0L
            else current[key] == expectedOldValue
        if (matches) {
            Mutation(current.withMapping(key, newValue), true)
        } else {
            Mutation(current, false)
        }
    }

    private inline fun <R> mutate(key: K, operation: (Map<K, Long>) -> Mutation<K, R>): R =
        stripeFor(key).mutate(operation)

    private fun stripeFor(key: K): Stripe<K> = stripes[stripeIndex(key)]

    private fun snapshot(): Map<K, Long> = buildMap {
        stripes.forEach { putAll(it.snapshot()) }
    }

    private fun Map<K, Long>.withMapping(key: K, value: Long): Map<K, Long> =
        LinkedHashMap(this).apply { put(key, value) }.toMap()

    private fun Map<K, Long>.withoutMapping(key: K): Map<K, Long> =
        LinkedHashMap(this).apply { remove(key) }.toMap()

    private inner class LiveMapView : AbstractMutableMap<K, Long>() {
        override val entries: MutableSet<MutableMap.MutableEntry<K, Long>>
            get() = entrySetView
        override val keys: MutableSet<K>
            get() = keySetView
        override val values: MutableCollection<Long>
            get() = valuesView
        override val size: Int
            get() = this@AtomicLongMap.size()

        override fun get(key: K): Long? = stripeFor(key).snapshot()[key]
        override fun containsKey(key: K): Boolean = this@AtomicLongMap.containsKey(key)
        override fun containsValue(value: Long): Boolean =
            stripes.any { it.snapshot().containsValue(value) }
        override fun isEmpty(): Boolean = this@AtomicLongMap.isEmpty()
        override fun put(key: K, value: Long): Long? = readOnlyView()
        override fun putAll(from: Map<out K, Long>): Unit = readOnlyView()
        override fun remove(key: K): Long? = readOnlyView()
        override fun clear(): Unit = readOnlyView()
    }

    private inner class LiveEntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, Long>>() {
        override val size: Int
            get() = this@AtomicLongMap.size()

        override fun contains(element: MutableMap.MutableEntry<K, Long>): Boolean =
            stripeFor(element.key).snapshot().entries.any {
                it.key == element.key && it.value == element.value
            }

        override fun add(element: MutableMap.MutableEntry<K, Long>): Boolean = readOnlyView()
        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, Long>>): Boolean = readOnlyView()
        override fun remove(element: MutableMap.MutableEntry<K, Long>): Boolean = readOnlyView()
        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, Long>>): Boolean = readOnlyView()
        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, Long>>): Boolean = readOnlyView()
        override fun clear(): Unit = readOnlyView()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Long>> {
            val snapshot = snapshot().entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<K, Long>> {
                override fun hasNext(): Boolean = snapshot.hasNext()
                override fun next(): MutableMap.MutableEntry<K, Long> {
                    val entry = snapshot.next()
                    return ImmutableEntry(entry.key, entry.value)
                }
                override fun remove(): Unit = readOnlyView()
            }
        }
    }

    private inner class LiveKeySet : AbstractMutableSet<K>() {
        override val size: Int
            get() = this@AtomicLongMap.size()

        override fun contains(element: K): Boolean = this@AtomicLongMap.containsKey(element)
        override fun add(element: K): Boolean = readOnlyView()
        override fun addAll(elements: Collection<K>): Boolean = readOnlyView()
        override fun remove(element: K): Boolean = readOnlyView()
        override fun removeAll(elements: Collection<K>): Boolean = readOnlyView()
        override fun retainAll(elements: Collection<K>): Boolean = readOnlyView()
        override fun clear(): Unit = readOnlyView()

        override fun iterator(): MutableIterator<K> = readOnlySnapshotIterator(snapshot().keys.iterator())
    }

    private inner class LiveValues : AbstractMutableCollection<Long>() {
        override val size: Int
            get() = this@AtomicLongMap.size()

        override fun contains(element: Long): Boolean =
            stripes.any { it.snapshot().containsValue(element) }
        override fun add(element: Long): Boolean = readOnlyView()
        override fun addAll(elements: Collection<Long>): Boolean = readOnlyView()
        override fun remove(element: Long): Boolean = readOnlyView()
        override fun removeAll(elements: Collection<Long>): Boolean = readOnlyView()
        override fun retainAll(elements: Collection<Long>): Boolean = readOnlyView()
        override fun clear(): Unit = readOnlyView()

        override fun iterator(): MutableIterator<Long> = readOnlySnapshotIterator(snapshot().values.iterator())
    }

    private fun <T> readOnlySnapshotIterator(snapshot: Iterator<T>): MutableIterator<T> =
        object : MutableIterator<T> {
            override fun hasNext(): Boolean = snapshot.hasNext()
            override fun next(): T = snapshot.next()
            override fun remove(): Unit = readOnlyView()
        }

    private class ImmutableEntry<K : Any>(
        override val key: K,
        override val value: Long,
    ) : MutableMap.MutableEntry<K, Long> {
        override fun setValue(newValue: Long): Long = readOnlyView()

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }

    private class Mutation<K : Any, R>(val values: Map<K, Long>, val result: R)

    @OptIn(ExperimentalAtomicApi::class)
    private class Stripe<K : Any>(initialValues: Map<K, Long>) {
        private val state = AtomicReference(initialValues)
        private val writerActive = AtomicBoolean(false)

        fun snapshot(): Map<K, Long> = state.load()

        inline fun <R> mutate(operation: (Map<K, Long>) -> Mutation<K, R>): R {
            acquireWriter()
            try {
                val mutation = operation(state.load())
                state.store(mutation.values)
                return mutation.result
            } finally {
                writerActive.store(false)
            }
        }

        fun acquireWriter() {
            while (!writerActive.compareAndSet(false, true)) {
                // Updates are deliberately short and non-suspending. Reads remain lock-free.
            }
        }
    }

    companion object {
        private const val STRIPE_COUNT = 32

        private fun stripeIndex(key: Any): Int {
            val hash = key.hashCode()
            return (hash xor (hash ushr 16)) and (STRIPE_COUNT - 1)
        }

        private fun <T> readOnlyView(): T =
            throw UnsupportedOperationException("AtomicLongMap.asMap() is read-only")

        fun <K : Any> create(): AtomicLongMap<K> = AtomicLongMap(emptyMap())

        fun <K : Any> create(values: Map<out K, Long>): AtomicLongMap<K> =
            AtomicLongMap(values.entries.associateTo(LinkedHashMap()) { it.key to it.value })
    }
}
