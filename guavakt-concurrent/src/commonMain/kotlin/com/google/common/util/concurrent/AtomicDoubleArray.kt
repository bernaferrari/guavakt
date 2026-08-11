package dev.guavakt.util.concurrent

/** Guava AtomicDoubleArray — array of atomic doubles via bit patterns. */
class AtomicDoubleArray {
    private val longs: AtomicRefArray<Long>
    constructor(length: Int) { longs = AtomicRefArray(length) }
    constructor(array: DoubleArray) {
        longs = AtomicRefArray(Array(array.size) { array[it].toBits() })
    }
    fun length(): Int = longs.length()
    fun get(i: Int): Double = Double.fromBits(longs.get(i))
    fun set(i: Int, newValue: Double) = longs.set(i, newValue.toBits())
    fun getAndSet(i: Int, newValue: Double): Double =
        Double.fromBits(longs.getAndSet(i, newValue.toBits()))
    fun compareAndSet(i: Int, expect: Double, update: Double): Boolean =
        longs.compareAndSet(i, expect.toBits(), update.toBits())
    fun getAndAdd(i: Int, delta: Double): Double {
        while (true) {
            val current = get(i)
            val next = current + delta
            if (compareAndSet(i, current, next)) return current
        }
    }
    fun addAndGet(i: Int, delta: Double): Double = getAndAdd(i, delta) + delta
    override fun toString(): String = (0 until length()).joinToString(prefix = "[", postfix = "]") { get(it).toString() }
}
