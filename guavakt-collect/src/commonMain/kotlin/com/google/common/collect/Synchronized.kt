package dev.guavakt.collect

/**
 * Guava Synchronized — synchronized wrappers.
 * KMP commonMain has no monitor `synchronized`; uses a reentrant-style lock object with
 * critical sections serialized via a platform-agnostic lock holder (single-threaded safe;
 * multi-threaded JVM users should prefer java.util.Collections.synchronized* on JVM actuals).
 */
internal object Synchronized {
    /** Minimal lock: exclusive critical section for cooperative / single-threaded KMP. */
    private class Lock {
        private var held = false
        fun <T> withLock(block: () -> T): T {
            // Spin-less exclusive flag; sufficient for KMP common contracts without JVM monitors.
            check(!held) { "reentrant synchronized not supported on KMP commonMain" }
            held = true
            try {
                return block()
            } finally {
                held = false
            }
        }
    }

    fun <E> collection(delegate: MutableCollection<E>, mutex: Any? = null): MutableCollection<E> {
        val lock = Lock()
        return object : MutableCollection<E> {
            override val size: Int get() = lock.withLock { delegate.size }
            override fun isEmpty(): Boolean = lock.withLock { delegate.isEmpty() }
            override fun contains(element: E): Boolean = lock.withLock { delegate.contains(element) }
            override fun containsAll(elements: Collection<E>): Boolean = lock.withLock { delegate.containsAll(elements) }
            override fun add(element: E): Boolean = lock.withLock { delegate.add(element) }
            override fun remove(element: E): Boolean = lock.withLock { delegate.remove(element) }
            override fun addAll(elements: Collection<E>): Boolean = lock.withLock { delegate.addAll(elements) }
            override fun removeAll(elements: Collection<E>): Boolean = lock.withLock { delegate.removeAll(elements) }
            override fun retainAll(elements: Collection<E>): Boolean = lock.withLock { delegate.retainAll(elements) }
            override fun clear() = lock.withLock { delegate.clear() }
            override fun iterator(): MutableIterator<E> = lock.withLock { delegate.toMutableList().iterator() }
        }
    }

    fun <K, V> map(delegate: MutableMap<K, V>, mutex: Any? = null): MutableMap<K, V> {
        val lock = Lock()
        return object : MutableMap<K, V> {
            override val size: Int get() = lock.withLock { delegate.size }
            override fun isEmpty(): Boolean = lock.withLock { delegate.isEmpty() }
            override fun containsKey(key: K): Boolean = lock.withLock { delegate.containsKey(key) }
            override fun containsValue(value: V): Boolean = lock.withLock { delegate.containsValue(value) }
            override fun get(key: K): V? = lock.withLock { delegate[key] }
            override fun put(key: K, value: V): V? = lock.withLock { delegate.put(key, value) }
            override fun remove(key: K): V? = lock.withLock { delegate.remove(key) }
            override fun putAll(from: Map<out K, V>) = lock.withLock { delegate.putAll(from) }
            override fun clear() = lock.withLock { delegate.clear() }
            override val keys: MutableSet<K> get() = lock.withLock { delegate.keys.toMutableSet() }
            override val values: MutableCollection<V> get() = lock.withLock { delegate.values.toMutableList() }
            override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
                get() = lock.withLock { delegate.entries.toMutableSet() }
        }
    }
}
