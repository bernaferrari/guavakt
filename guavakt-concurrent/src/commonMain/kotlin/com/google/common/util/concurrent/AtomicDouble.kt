package dev.guavakt.util.concurrent

/**
 * Guava AtomicDouble — atomic double via long bits (doubleToRawLongBits pattern).
 */
class AtomicDouble(initialValue: Double = 0.0) : Number() {
    private val bits = AtomicRef(initialValue.toRawBits())

    fun get(): Double = Double.fromBits(bits.get())
    fun set(newValue: Double) = bits.set(newValue.toRawBits())
    fun lazySet(newValue: Double) = set(newValue)
    fun getAndSet(newValue: Double): Double = Double.fromBits(bits.getAndSet(newValue.toRawBits()))
    fun compareAndSet(expect: Double, update: Double): Boolean =
        bits.compareAndSet(expect.toRawBits(), update.toRawBits())

    /** Portable atomics do not introduce spurious failure, which is permitted by Guava's weak CAS. */
    fun weakCompareAndSet(expect: Double, update: Double): Boolean = compareAndSet(expect, update)

    fun getAndAdd(delta: Double): Double = getAndAccumulate(delta, Double::plus)
    fun addAndGet(delta: Double): Double = accumulateAndGet(delta, Double::plus)

    /** Atomically returns the old value after applying [update] to it. */
    fun getAndUpdate(update: (Double) -> Double): Double {
        while (true) {
            val currentBits = bits.get()
            val current = Double.fromBits(currentBits)
            val next = update(current)
            if (bits.compareAndSet(currentBits, next.toRawBits())) return current
        }
    }

    /** Atomically returns the updated value after applying [update] to the old value. */
    fun updateAndGet(update: (Double) -> Double): Double {
        while (true) {
            val currentBits = bits.get()
            val current = Double.fromBits(currentBits)
            val next = update(current)
            if (bits.compareAndSet(currentBits, next.toRawBits())) return next
        }
    }

    /** Atomically returns the old value after applying [accumulator] with [value]. */
    fun getAndAccumulate(value: Double, accumulator: (Double, Double) -> Double): Double =
        getAndUpdate { current -> accumulator(current, value) }

    /** Atomically returns the new value after applying [accumulator] with [value]. */
    fun accumulateAndGet(value: Double, accumulator: (Double, Double) -> Double): Double =
        updateAndGet { current -> accumulator(current, value) }

    override fun toByte(): Byte = get().toInt().toByte()
    override fun toDouble(): Double = get()
    override fun toFloat(): Float = get().toFloat()
    override fun toInt(): Int = get().toInt()
    override fun toLong(): Long = get().toLong()
    override fun toShort(): Short = get().toInt().toShort()

    override fun toString(): String = get().toString()
}
