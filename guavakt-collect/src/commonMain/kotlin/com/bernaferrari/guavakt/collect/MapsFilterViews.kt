package com.bernaferrari.guavakt.collect

/** Live map view filtered by key predicate. */
internal class FilteredKeyMapView<K, V>(
    private val unfiltered: MutableMap<K, V>,
    private val keyPredicate: (K) -> Boolean,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = unfiltered.keys.count(keyPredicate)
    override fun containsKey(key: K): Boolean = keyPredicate(key) && unfiltered.containsKey(key)
    override fun get(key: K): V? = if (keyPredicate(key)) unfiltered[key] else null
    override fun put(key: K, value: V): V? {
        require(keyPredicate(key)) { "Key rejected by filter" }
        return unfiltered.put(key, value)
    }
    override fun remove(key: K): V? = if (keyPredicate(key)) unfiltered.remove(key) else null
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = FilteredEntries { keyPredicate(it.key) }

    private inner class FilteredEntries(
        private val ok: (MutableMap.MutableEntry<K, V>) -> Boolean,
    ) : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = unfiltered.entries.count(ok)
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val it = unfiltered.entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                private var next: MutableMap.MutableEntry<K, V>? = null
                private var ready = false
                private var last: MutableMap.MutableEntry<K, V>? = null
                private fun advance() {
                    while (it.hasNext()) {
                        val e = it.next()
                        if (ok(e)) {
                            next = e
                            ready = true
                            return
                        }
                    }
                    next = null
                    ready = false
                }
                override fun hasNext(): Boolean {
                    if (!ready) advance()
                    return ready
                }
                override fun next(): MutableMap.MutableEntry<K, V> {
                    if (!hasNext()) throw NoSuchElementException()
                    last = next
                    ready = false
                    return last!!
                }
                override fun remove() {
                    val e = last ?: throw IllegalStateException()
                    it.remove()
                    last = null
                }
            }
        }
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            put(element.key, element.value); return true
        }
        override fun clear() {
            val keys = unfiltered.entries.filter(ok).map { it.key }.toList()
            for (k in keys) unfiltered.remove(k)
        }
    }
}

internal class FilteredValueMapView<K, V>(
    private val unfiltered: MutableMap<K, V>,
    private val valuePredicate: (V) -> Boolean,
) : AbstractMutableMap<K, V>() {
    override val size: Int get() = unfiltered.values.count(valuePredicate)
    override fun containsKey(key: K): Boolean = unfiltered[key]?.let(valuePredicate) == true
    override fun get(key: K): V? = unfiltered[key]?.takeIf(valuePredicate)
    override fun put(key: K, value: V): V? {
        require(valuePredicate(value)) { "Value rejected by filter" }
        return unfiltered.put(key, value)
    }
    override fun remove(key: K): V? {
        val v = unfiltered[key] ?: return null
        if (!valuePredicate(v)) return null
        return unfiltered.remove(key)
    }
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = this@FilteredValueMapView.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val it = unfiltered.entries.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var next: MutableMap.MutableEntry<K, V>? = null
                    private var ready = false
                    private var last: MutableMap.MutableEntry<K, V>? = null
                    private fun advance() {
                        while (it.hasNext()) {
                            val e = it.next()
                            if (valuePredicate(e.value)) {
                                next = e; ready = true; return
                            }
                        }
                        next = null; ready = false
                    }
                    override fun hasNext(): Boolean { if (!ready) advance(); return ready }
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        if (!hasNext()) throw NoSuchElementException()
                        last = next; ready = false; return last!!
                    }
                    override fun remove() {
                        last ?: throw IllegalStateException()
                        it.remove(); last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                put(element.key, element.value); return true
            }
            override fun clear() {
                for ((k, v) in unfiltered.entries.toList()) if (valuePredicate(v)) unfiltered.remove(k)
            }
        }
}

internal class FilteredEntryMapView<K, V>(
    private val unfiltered: MutableMap<K, V>,
    private val entryPredicate: (Map.Entry<K, V>) -> Boolean,
) : AbstractMutableMap<K, V>() {
    private fun ok(k: K, v: V): Boolean =
        entryPredicate(object : Map.Entry<K, V> {
            override val key: K get() = k
            override val value: V get() = v
        })
    override val size: Int get() = unfiltered.entries.count(entryPredicate)
    override fun get(key: K): V? = unfiltered[key]?.takeIf { ok(key, it) }
    override fun put(key: K, value: V): V? {
        require(ok(key, value)) { "Entry rejected by filter" }
        return unfiltered.put(key, value)
    }
    override fun remove(key: K): V? {
        val v = unfiltered[key] ?: return null
        if (!ok(key, v)) return null
        return unfiltered.remove(key)
    }
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
            override val size: Int get() = this@FilteredEntryMapView.size
            override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
                val it = unfiltered.entries.iterator()
                return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var next: MutableMap.MutableEntry<K, V>? = null
                    private var ready = false
                    private var last: MutableMap.MutableEntry<K, V>? = null
                    private fun advance() {
                        while (it.hasNext()) {
                            val e = it.next()
                            if (entryPredicate(e)) {
                                next = e; ready = true; return
                            }
                        }
                        next = null; ready = false
                    }
                    override fun hasNext(): Boolean { if (!ready) advance(); return ready }
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        if (!hasNext()) throw NoSuchElementException()
                        last = next; ready = false; return last!!
                    }
                    override fun remove() {
                        last ?: throw IllegalStateException()
                        it.remove(); last = null
                    }
                }
            }
            override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
                put(element.key, element.value); return true
            }
            override fun clear() {
                for (e in unfiltered.entries.filter(entryPredicate).toList()) unfiltered.remove(e.key)
            }
        }
}
