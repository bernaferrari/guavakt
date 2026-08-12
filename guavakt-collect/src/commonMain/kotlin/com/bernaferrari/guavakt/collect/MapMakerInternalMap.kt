package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.base.internal.PlatformSoftRef
import com.bernaferrari.guavakt.base.internal.PlatformWeakRef
import com.bernaferrari.guavakt.base.internal.Strength
import com.bernaferrari.guavakt.base.internal.platformIdentityHashCode
import com.bernaferrari.guavakt.base.internal.pollClearedWeakOrSoftReferences

/**
 * Guava MapMakerInternalMap — internal map with optional weak keys / weak|soft values.
 */
@Suppress("UNCHECKED_CAST")
open class MapMakerInternalMap<K : Any, V : Any> private constructor(
    private val keyStrength: Strength,
    private val valueStrength: Strength,
) : AbstractMutableMap<K, V>() {
    private val strong = LinkedHashMap<K, Any>() // value is V or Platform*Ref
    private val weakBuckets = LinkedHashMap<Int, MutableList<WeakKeyEntry<K, V>>>()

    private class WeakKeyEntry<K : Any, V : Any>(
        val keyRef: PlatformWeakRef<K>,
        var valueBox: Any,
        val idHash: Int,
    )

    private fun boxValue(v: V): Any = when (valueStrength) {
        Strength.STRONG -> v
        Strength.WEAK -> PlatformWeakRef(v)
        Strength.SOFT -> PlatformSoftRef(v)
    }

    private fun unboxValue(box: Any?): V? {
        if (box == null) return null
        return when (valueStrength) {
            Strength.STRONG -> box as V
            Strength.WEAK -> (box as PlatformWeakRef<V>).get()
            Strength.SOFT -> (box as PlatformSoftRef<V>).get()
        }
    }

    private fun usesWeakKeys() = keyStrength == Strength.WEAK

    override val size: Int
        get() {
            cleanUp()
            return if (usesWeakKeys()) weakBuckets.values.sumOf { it.size } else strong.size
        }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            cleanUp()
            val snap = LinkedHashMap<K, V>()
            if (usesWeakKeys()) {
                for (bucket in weakBuckets.values) for (e in bucket) {
                    val k = e.keyRef.get() ?: continue
                    val v = unboxValue(e.valueBox) ?: continue
                    snap[k] = v
                }
            } else {
                for ((k, box) in strong) {
                    val v = unboxValue(box) ?: continue
                    snap[k] = v
                }
            }
            return snap.entries
        }

    override fun get(key: K): V? {
        cleanUp()
        if (!usesWeakKeys()) return unboxValue(strong[key])
        val bucket = weakBuckets[platformIdentityHashCode(key)] ?: return null
        for (e in bucket) {
            val live = e.keyRef.get()
            if (live === key || (live != null && live == key)) return unboxValue(e.valueBox)
        }
        return null
    }

    override fun containsKey(key: K): Boolean = get(key) != null

    override fun put(key: K, value: V): V? {
        cleanUp()
        val prev = get(key)
        if (usesWeakKeys()) {
            val id = platformIdentityHashCode(key)
            val bucket = weakBuckets.getOrPut(id) { ArrayList() }
            bucket.removeAll { it.keyRef.get() == null || it.keyRef.get() === key || it.keyRef.get() == key }
            bucket.add(WeakKeyEntry(PlatformWeakRef(key), boxValue(value), id))
        } else {
            strong[key] = boxValue(value)
        }
        return prev
    }

    override fun remove(key: K): V? {
        cleanUp()
        val prev = get(key)
        if (usesWeakKeys()) {
            val id = platformIdentityHashCode(key)
            weakBuckets[id]?.removeAll {
                val live = it.keyRef.get()
                live === key || live == key
            }
        } else {
            strong.remove(key)
        }
        return prev
    }

    override fun clear() {
        strong.clear()
        weakBuckets.clear()
    }

    fun cleanUp() {
        pollClearedWeakOrSoftReferences()
        if (usesWeakKeys()) {
            val it = weakBuckets.entries.iterator()
            while (it.hasNext()) {
                val (_, bucket) = it.next()
                bucket.removeAll { it.keyRef.get() == null || unboxValue(it.valueBox) == null }
                if (bucket.isEmpty()) it.remove()
            }
        } else if (valueStrength != Strength.STRONG) {
            val it = strong.entries.iterator()
            while (it.hasNext()) {
                if (unboxValue(it.next().value) == null) it.remove()
            }
        }
    }

    companion object {
        fun <K : Any, V : Any> create(
            initialCapacity: Int = 16,
            keyStrength: Strength = Strength.STRONG,
            valueStrength: Strength = Strength.STRONG,
        ): MapMakerInternalMap<K, V> = MapMakerInternalMap(keyStrength, valueStrength)
    }
}
