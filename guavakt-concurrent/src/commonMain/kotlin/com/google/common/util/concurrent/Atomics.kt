package dev.guavakt.util.concurrent

/**
 * Guava Atomics — factories for atomic references (KMP uses kotlin.concurrent atomics where available,
 * else synchronized holders).
 */
object Atomics {
    fun <V> newReference(): AtomicRef<V?> = AtomicRef(null)
    fun <V> newReference(initialValue: V): AtomicRef<V> = AtomicRef(initialValue)
    fun <V> newReferenceArray(length: Int): AtomicRefArray<V?> = AtomicRefArray(length)
    fun <V> newReferenceArray(array: Array<V>): AtomicRefArray<V> = AtomicRefArray(array)
}

class AtomicRef<V>(initial: V) {
    private var value: V = initial
    private val lock = Any()
    fun get(): V = monitorSync(lock) { value }
    fun set(newValue: V) = monitorSync(lock) { value = newValue }
    fun getAndSet(newValue: V): V = monitorSync(lock) { val old = value; value = newValue; old }
    fun compareAndSet(expect: V, update: V): Boolean = monitorSync(lock) {
        if (value != expect) return false
        value = update
        true
    }
    fun lazySet(newValue: V) = set(newValue)
    override fun toString(): String = get().toString()
}

class AtomicRefArray<V> {
    private val array: Array<Any?>
    private val lock = Any()
    constructor(length: Int) { array = arrayOfNulls(length) }
    constructor(copy: Array<V>) { array = Array(copy.size) { copy[it] as Any? } }
    fun length(): Int = array.size
    @Suppress("UNCHECKED_CAST")
    fun get(i: Int): V = monitorSync(lock) { array[i] as V }
    fun set(i: Int, newValue: V) = monitorSync(lock) { array[i] = newValue }
    @Suppress("UNCHECKED_CAST")
    fun getAndSet(i: Int, newValue: V): V = monitorSync(lock) {
        val old = array[i] as V; array[i] = newValue; old
    }
    fun compareAndSet(i: Int, expect: V, update: V): Boolean = monitorSync(lock) {
        if (array[i] != expect) return false
        array[i] = update
        true
    }
}
