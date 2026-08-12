package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.internal.PlatformSoftRef
import com.bernaferrari.guavakt.base.internal.PlatformWeakRef
import com.bernaferrari.guavakt.base.internal.Strength
import com.bernaferrari.guavakt.base.internal.platformIdentityHashCode
import com.bernaferrari.guavakt.base.internal.pollClearedWeakOrSoftReferences
import com.bernaferrari.guavakt.base.internal.platformSupportsWeakReferences

/**
 * Guava MapMaker — concurrent-ish map builder with optional weak/soft values and weak keys.
 * JVM: real weak/soft references via the base module's platform bridge; other targets: strong stand-ins.
 */
class MapMaker {
    private var initialCapacity = 16
    private var concurrencyLevel = 4
    private var keyStrength: Strength = Strength.STRONG
    private var valueStrength: Strength = Strength.STRONG

    fun initialCapacity(capacity: Int): MapMaker = apply {
        require(capacity >= 0)
        initialCapacity = capacity
    }

    fun concurrencyLevel(level: Int): MapMaker = apply {
        require(level > 0)
        concurrencyLevel = level
    }

    fun weakKeys(): MapMaker = apply { keyStrength = Strength.WEAK }
    fun weakValues(): MapMaker = apply { valueStrength = Strength.WEAK }
    fun softValues(): MapMaker = apply { valueStrength = Strength.SOFT }

    fun <K : Any, V : Any> makeMap(): MutableMap<K, V> =
        MapMakerInternalMap.create(initialCapacity, keyStrength, valueStrength)

    fun <K : Any, V : Any> makeComputingMap(computer: (K) -> V): MutableMap<K, V> =
        ComputingMap(computer, keyStrength, valueStrength)

    private class ComputingMap<K : Any, V : Any>(
        private val computer: (K) -> V,
        private val keyStrength: Strength,
        private val valueStrength: Strength,
    ) : AbstractMutableMap<K, V>() {
        private val delegate = MapMakerInternalMap.create<K, V>(16, keyStrength, valueStrength)
        override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = delegate.entries
        override val size: Int get() = delegate.size
        override fun get(key: K): V? {
            delegate[key]?.let { return it }
            val computed = computer(key)
            delegate[key] = computed
            return computed
        }
        override fun put(key: K, value: V): V? = delegate.put(key, value)
        override fun remove(key: K): V? = delegate.remove(key)
        override fun clear() = delegate.clear()
    }
}
