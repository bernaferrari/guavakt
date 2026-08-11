package dev.guavakt.collect

/** Guava Count — mutable int holder used by multiset implementations. */
internal class Count(private var value: Int = 0) {
    fun get(): Int = value
    fun add(delta: Int) { value += delta }
    fun addAndGet(delta: Int): Int { value += delta; return value }
    fun set(newValue: Int) { value = newValue }
    fun getAndSet(newValue: Int): Int { val old = value; value = newValue; return old }
    override fun hashCode(): Int = value
    override fun equals(other: Any?): Boolean = other is Count && other.value == value
    override fun toString(): String = value.toString()
}
