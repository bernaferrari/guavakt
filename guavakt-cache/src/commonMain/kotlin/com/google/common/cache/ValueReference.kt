package dev.guavakt.cache

/** Holds a cache value with the configured [Strength]. */
internal sealed class ValueHolder<V : Any> {
    abstract fun get(): V?
    abstract fun clear()

    class Strong<V : Any>(private var value: V?) : ValueHolder<V>() {
        override fun get(): V? = value
        override fun clear() { value = null }
    }

    class Weak<V : Any>(referent: V) : ValueHolder<V>() {
        private val ref = PlatformWeakRef(referent)
        override fun get(): V? = ref.get()
        override fun clear() = ref.clear()
    }

    class Soft<V : Any>(referent: V) : ValueHolder<V>() {
        private val ref = PlatformSoftRef(referent)
        override fun get(): V? = ref.get()
        override fun clear() = ref.clear()
    }

    companion object {
        fun <V : Any> create(value: V, strength: Strength): ValueHolder<V> = when (strength) {
            Strength.STRONG -> Strong(value)
            Strength.WEAK -> Weak(value)
            Strength.SOFT -> Soft(value)
        }
    }
}
