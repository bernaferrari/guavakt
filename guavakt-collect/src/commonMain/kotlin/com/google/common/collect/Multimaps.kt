package dev.guavakt.collect

/**
 * Guava Multimaps — factories and views.
 * Prefer concrete types ([ArrayListMultimap], [HashMultimap], [LinkedListMultimap], [TreeMultimap])
 * or [MultimapBuilder] for new code.
 */
object Multimaps {

    fun <K, V> newListMultimap(
        map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
        factory: () -> MutableList<V> = { ArrayList() },
    ): ListMultimap<K, V> = object : AbstractListMultimap<K, V>(map) {
        override fun createCollection(): MutableList<V> = factory()
    }

    fun <K, V> newSetMultimap(
        map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
        factory: () -> MutableSet<V> = { LinkedHashSet() },
    ): SetMultimap<K, V> = object : AbstractSetMultimap<K, V>(map) {
        override fun createCollection(): MutableSet<V> = factory()
    }

    fun <K, V> newMultimap(
        map: MutableMap<K, MutableCollection<V>> = LinkedHashMap(),
        factory: () -> MutableCollection<V> = { ArrayList() },
    ): Multimap<K, V> = object : AbstractMapBasedMultimap<K, V>(map) {
        override fun createCollection(): MutableCollection<V> = factory()
    }

    /** Guava [index] — builds an immutable list multimap (Kotlin lists inside). */
    fun <K, V> index(values: Iterable<V>, keyFunction: (V) -> K): ImmutableListMultimap<K, V> {
        val builder = ImmutableListMultimap.builder<K, V>()
        for (v in values) builder.put(keyFunction(v), v)
        return builder.build()
    }

    /**
     * Lazy live transforming view. Removals through the transformed values, entries, and map
     * views update [fromMultimap]; additions and replacement are unsupported because [function]
     * has no inverse.
     *
     * This compatibility overload retains GuavaKt's original [ListMultimap] return shape even
     * when [fromMultimap] is a general multimap. The list preserves the backing iteration order.
     */
    fun <K, V1, V2> transformValues(
        fromMultimap: Multimap<K, V1>,
        function: (V1) -> V2,
    ): ListMultimap<K, V2> = TransformedListMultimapView(fromMultimap) { _, value -> function(value) }

    /** Key-aware variant of [transformValues], matching Guava's lazy removal-capable view. */
    fun <K, V1, V2> transformEntries(
        fromMultimap: Multimap<K, V1>,
        transformer: (K, V1) -> V2,
    ): ListMultimap<K, V2> = TransformedListMultimapView(fromMultimap, transformer)

    fun <K, V> invertFrom(source: Multimap<V, K>, dest: Multimap<K, V>): Multimap<K, V> {
        for (e in source.entries()) dest.put(e.value, e.key)
        return dest
    }

    /**
     * Guava [forMap] — multimap view of a map (one value per key).
     * If [map] is a [MutableMap], the view is **live** (mutations go both ways);
     * otherwise returns a [HashMultimap] snapshot (Kotlin read-only maps).
     */
    fun <K, V> forMap(map: Map<K, V>): SetMultimap<K, V> {
        if (map is MutableMap<K, V>) return MapAsSetMultimap(map)
        val m = HashMultimap.create<K, V>()
        for ((k, v) in map) m.put(k, v)
        return m
    }

    private class MapAsSetMultimap<K, V>(
        private val map: MutableMap<K, V>,
    ) : SetMultimap<K, V> {
        override fun size(): Int = map.size
        override fun isEmpty(): Boolean = map.isEmpty()
        override fun containsKey(key: Any?): Boolean = map.containsKey(key)
        override fun containsValue(value: Any?): Boolean = map.containsValue(value)
        override fun containsEntry(key: Any?, value: Any?): Boolean {
            @Suppress("UNCHECKED_CAST")
            val k = key as? K ?: return false
            return map[k] == value
        }
        override fun get(key: K): MutableSet<V> = object : AbstractMutableSet<V>() {
            override val size: Int get() = if (map.containsKey(key)) 1 else 0
            override fun iterator(): MutableIterator<V> {
                if (!map.containsKey(key)) return mutableListOf<V>().iterator()
                val v = map[key] as V
                return object : MutableIterator<V> {
                    private var done = false
                    private var removed = false
                    override fun hasNext() = !done
                    override fun next(): V {
                        if (done) throw NoSuchElementException()
                        done = true
                        return v
                    }
                    override fun remove() {
                        if (!done || removed) throw IllegalStateException()
                        map.remove(key)
                        removed = true
                    }
                }
            }
            override fun add(element: V): Boolean {
                val had = map.containsKey(key)
                val old = map.put(key, element)
                return !had || old != element
            }
            override fun contains(element: V): Boolean =
                map.containsKey(key) && map[key] == element
            override fun remove(element: V): Boolean {
                if (!map.containsKey(key) || map[key] != element) return false
                map.remove(key)
                return true
            }
            override fun clear() { map.remove(key) }
        }
        override fun keySet(): Set<K> = map.keys
        override fun keys(): Multiset<K> = object : AbstractMutableCollection<K>(), Multiset<K> {
            override val size: Int get() = map.size
            override fun iterator(): MutableIterator<K> = map.keys.iterator()
            override fun add(element: K): Boolean = throw UnsupportedOperationException()
            override fun count(element: Any?): Int = if (map.containsKey(element)) 1 else 0
            override fun add(element: K, occurrences: Int): Int {
                throw UnsupportedOperationException()
            }
            override fun remove(element: Any?, occurrences: Int): Int {
                @Suppress("UNCHECKED_CAST")
                val k = element as? K ?: return 0
                if (!map.containsKey(k)) return 0
                if (occurrences > 0) map.remove(k)
                return 1
            }
            override fun setCount(element: K, count: Int): Int {
                val old = count(element)
                when {
                    count == 0 -> map.remove(element)
                    count == 1 && old == 0 -> throw UnsupportedOperationException("cannot add via keys()")
                    count > 1 -> throw IllegalArgumentException("forMap keys multiset max count is 1")
                }
                return old
            }
            override fun elementSet(): Set<K> = map.keys
            override fun entrySet(): Set<Multiset.Entry<K>> =
                map.keys.map { k ->
                    object : Multiset.Entry<K> {
                        override fun getElement(): K = k
                        override fun getCount(): Int = 1
                    }
                }.toSet()
        }
        override fun values(): Collection<V> = map.values
        override fun entries(): Set<Map.Entry<K, V>> = map.entries
        override fun asMap(): Map<K, Set<V>> = object : AbstractMutableMap<K, Set<V>>() {
            override val size: Int get() = map.size
            override fun containsKey(key: K): Boolean = map.containsKey(key)
            override fun get(key: K): Set<V>? =
                if (map.containsKey(key)) setOf(map[key] as V) else null
            override fun put(key: K, value: Set<V>): Set<V>? {
                val old = if (map.containsKey(key)) setOf(map[key] as V) else null
                require(value.size == 1) { "forMap asMap values must be singleton sets" }
                map[key] = value.first()
                return old
            }
            override fun remove(key: K): Set<V>? {
                if (!map.containsKey(key)) return null
                return setOf(map.remove(key) as V)
            }
            override val entries: MutableSet<MutableMap.MutableEntry<K, Set<V>>>
                get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, Set<V>>>() {
                    override val size: Int get() = map.size
                    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Set<V>>> {
                        val it = map.entries.iterator()
                        return object : MutableIterator<MutableMap.MutableEntry<K, Set<V>>> {
                            private var lastKey: K? = null
                            override fun hasNext() = it.hasNext()
                            override fun next(): MutableMap.MutableEntry<K, Set<V>> {
                                val e = it.next()
                                lastKey = e.key
                                return object : MutableMap.MutableEntry<K, Set<V>> {
                                    override val key: K get() = e.key
                                    override val value: Set<V> get() = setOf(e.value)
                                    override fun setValue(newValue: Set<V>): Set<V> {
                                        require(newValue.size == 1)
                                        val old = setOf(e.value)
                                        map[e.key] = newValue.first()
                                        return old
                                    }
                                }
                            }
                            override fun remove() {
                                val k = lastKey ?: throw IllegalStateException()
                                map.remove(k)
                                lastKey = null
                            }
                        }
                    }
                    override fun add(element: MutableMap.MutableEntry<K, Set<V>>): Boolean {
                        put(element.key, element.value); return true
                    }
                }
        }
        override fun put(key: K, value: V): Boolean {
            val old = map.put(key, value)
            return old != value
        }
        override fun putAll(key: K, values: Iterable<V>): Boolean {
            val it = values.iterator()
            if (!it.hasNext()) return false
            var last = it.next()
            while (it.hasNext()) last = it.next()
            return put(key, last)
        }
        override fun putAll(multimap: Multimap<out K, out V>): Boolean {
            var changed = false
            for (e in multimap.entries()) if (put(e.key, e.value)) changed = true
            return changed
        }
        override fun remove(key: Any?, value: Any?): Boolean {
            @Suppress("UNCHECKED_CAST")
            val k = key as? K ?: return false
            if (map[k] != value) return false
            map.remove(k)
            return true
        }
        override fun removeAll(key: Any?): Set<V> {
            @Suppress("UNCHECKED_CAST")
            val k = key as? K ?: return emptySet()
            val v = map.remove(k) ?: return emptySet()
            return setOf(v)
        }
        override fun replaceValues(key: K, values: Iterable<V>): Set<V> {
            val old = removeAll(key)
            putAll(key, values)
            return old
        }
        override fun clear() = map.clear()
        override fun equals(other: Any?): Boolean =
            other is Multimap<*, *> && asMap() == other.asMap()
        override fun hashCode(): Int = asMap().hashCode()
    }

    fun <K, V> synchronizedMultimap(multimap: Multimap<K, V>): Multimap<K, V> = multimap

    fun <K, V> unmodifiableMultimap(delegate: Multimap<out K, out V>): Multimap<K, V> =
        object : Multimap<K, V> {
            @Suppress("UNCHECKED_CAST")
            private val d = delegate as Multimap<K, V>
            override fun size(): Int = d.size()
            override fun isEmpty(): Boolean = d.isEmpty()
            override fun containsKey(key: Any?): Boolean = d.containsKey(key)
            override fun containsValue(value: Any?): Boolean = d.containsValue(value)
            override fun containsEntry(key: Any?, value: Any?): Boolean = d.containsEntry(key, value)
            override fun get(key: K): MutableCollection<V> =
                unmodifiableMutableCollection(d.get(key))
            override fun keySet(): Set<K> = unmodifiableMutableSet(d.keySet())
            override fun keys(): Multiset<K> = Multisets.unmodifiableMultiset(d.keys())
            override fun values(): Collection<V> = unmodifiableMutableCollection(d.values())
            override fun entries(): Collection<Map.Entry<K, V>> = unmodifiableMutableCollection(d.entries())
            override fun asMap(): Map<K, Collection<V>> = object : AbstractMutableMap<K, Collection<V>>() {
                private fun backing(): Map<K, Collection<V>> = d.asMap()
                override val size: Int get() = backing().size
                override fun containsKey(key: K): Boolean = backing().containsKey(key)
                override fun get(key: K): Collection<V>? =
                    backing()[key]?.let(::unmodifiableMutableCollection)
                override fun put(key: K, value: Collection<V>): Collection<V>? =
                    throw UnsupportedOperationException()
                override fun remove(key: K): Collection<V>? = throw UnsupportedOperationException()
                override fun clear() = throw UnsupportedOperationException()
                override val entries: MutableSet<MutableMap.MutableEntry<K, Collection<V>>>
                    get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, Collection<V>>>() {
                        override val size: Int get() = backing().size
                        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> {
                            val iterator = backing().entries.iterator()
                            return object : MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> {
                                override fun hasNext(): Boolean = iterator.hasNext()
                                override fun next(): MutableMap.MutableEntry<K, Collection<V>> {
                                    val entry = iterator.next()
                                    return object : MutableMap.MutableEntry<K, Collection<V>> {
                                        override val key: K get() = entry.key
                                        override val value: Collection<V>
                                            get() = unmodifiableMutableCollection(entry.value)
                                        override fun setValue(newValue: Collection<V>): Collection<V> =
                                            throw UnsupportedOperationException()
                                        override fun equals(other: Any?): Boolean =
                                            other is Map.Entry<*, *> && key == other.key && value == other.value
                                        override fun hashCode(): Int =
                                            (key?.hashCode() ?: 0) xor value.hashCode()
                                        override fun toString(): String = "$key=$value"
                                    }
                                }
                                override fun remove() = throw UnsupportedOperationException()
                            }
                        }
                        override fun add(element: MutableMap.MutableEntry<K, Collection<V>>): Boolean =
                            throw UnsupportedOperationException()
                    }
            }
            override fun put(key: K, value: V): Boolean = throw UnsupportedOperationException()
            override fun putAll(key: K, values: Iterable<V>): Boolean = throw UnsupportedOperationException()
            override fun putAll(multimap: Multimap<out K, out V>): Boolean = throw UnsupportedOperationException()
            override fun remove(key: Any?, value: Any?): Boolean = throw UnsupportedOperationException()
            override fun removeAll(key: Any?): Collection<V> = throw UnsupportedOperationException()
            override fun replaceValues(key: K, values: Iterable<V>): Collection<V> =
                throw UnsupportedOperationException()
            override fun clear() = throw UnsupportedOperationException()
            override fun equals(other: Any?): Boolean = d == other
            override fun hashCode(): Int = d.hashCode()
        }

    fun <K, V> filterKeys(unfiltered: Multimap<K, V>, keyPredicate: (K) -> Boolean): Multimap<K, V> =
        filterEntries(unfiltered) { keyPredicate(it.key) }

    fun <K, V> filterKeys(
        unfiltered: ListMultimap<K, V>,
        keyPredicate: (K) -> Boolean,
    ): ListMultimap<K, V> =
        if (unfiltered is FilteredKeyListMultimapView<K, V>) {
            FilteredKeyListMultimapView(unfiltered.unfiltered) {
                unfiltered.keyPredicate(it) && keyPredicate(it)
            }
        } else {
            FilteredKeyListMultimapView(unfiltered, keyPredicate)
        }

    fun <K, V> filterKeys(
        unfiltered: SetMultimap<K, V>,
        keyPredicate: (K) -> Boolean,
    ): SetMultimap<K, V> = filterEntries(unfiltered) { keyPredicate(it.key) }

    fun <K, V> filterValues(unfiltered: Multimap<K, V>, valuePredicate: (V) -> Boolean): Multimap<K, V> =
        filterEntries(unfiltered) { valuePredicate(it.value) }

    fun <K, V> filterValues(
        unfiltered: SetMultimap<K, V>,
        valuePredicate: (V) -> Boolean,
    ): SetMultimap<K, V> = filterEntries(unfiltered) { valuePredicate(it.value) }

    fun <K, V> filterEntries(
        unfiltered: Multimap<K, V>,
        entryPredicate: (Map.Entry<K, V>) -> Boolean,
    ): Multimap<K, V> =
        if (unfiltered is FilteredEntryMultimapView<K, V>) {
            FilteredEntryMultimapView(unfiltered.unfiltered) {
                unfiltered.entryPredicate(it) && entryPredicate(it)
            }
        } else {
            FilteredEntryMultimapView(unfiltered, entryPredicate)
        }

    fun <K, V> filterEntries(
        unfiltered: SetMultimap<K, V>,
        entryPredicate: (Map.Entry<K, V>) -> Boolean,
    ): SetMultimap<K, V> =
        if (unfiltered is FilteredEntrySetMultimapView<K, V>) {
            FilteredEntrySetMultimapView(unfiltered.unfiltered) {
                unfiltered.entryPredicate(it) && entryPredicate(it)
            }
        } else {
            FilteredEntrySetMultimapView(unfiltered, entryPredicate)
        }
}
