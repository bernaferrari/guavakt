package com.bernaferrari.guavakt.collect

/**
 * Live entry-filtered multimap view.
 *
 * The predicate is checked lazily. Mutations that remove visible mappings write through to
 * [unfiltered], while additions are accepted only when the new entry satisfies [entryPredicate].
 * As in Guava, iterators obtained from filtered views do not support `remove`; the collection
 * mutation methods themselves do.
 */
internal class FilteredEntryMultimapView<K, V>(
    internal val unfiltered: Multimap<K, V>,
    internal val entryPredicate: (Map.Entry<K, V>) -> Boolean,
) : Multimap<K, V> {
    private fun entry(key: K, value: V): Map.Entry<K, V> = immutableEntry(key, value)

    private fun satisfies(key: K, value: V): Boolean = entryPredicate(entry(key, value))

    override fun size(): Int = entries().size

    override fun containsKey(key: Any?): Boolean =
        unfiltered.entries().any { it.key == key && satisfies(it.key, it.value) }

    override fun containsValue(value: Any?): Boolean =
        unfiltered.entries().any { it.value == value && satisfies(it.key, it.value) }

    override fun containsEntry(key: Any?, value: Any?): Boolean =
        unfiltered.entries().any {
            it.key == key && it.value == value && satisfies(it.key, it.value)
        }

    override fun get(key: K): MutableCollection<V> =
        filteredCollection(unfiltered.get(key)) { value -> satisfies(key, value) }

    override fun keySet(): Set<K> = FilteredKeySet()

    override fun keys(): Multiset<K> = FilteredKeysMultiset()

    override fun values(): Collection<V> = object : AbstractMutableCollection<V>() {
        override val size: Int get() = this@FilteredEntryMultimapView.size()

        override fun iterator(): MutableIterator<V> {
            val entries = this@FilteredEntryMultimapView.entries().iterator()
            return object : MutableIterator<V> {
                override fun hasNext(): Boolean = entries.hasNext()
                override fun next(): V = entries.next().value
                override fun remove(): Nothing = throw UnsupportedOperationException()
            }
        }

        override fun add(element: V): Nothing = throw UnsupportedOperationException()

        override fun remove(element: V): Boolean {
            val iterator = unfiltered.entries().iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (candidate.value == element && satisfies(candidate.key, candidate.value)) {
                    @Suppress("UNCHECKED_CAST")
                    (iterator as MutableIterator<Map.Entry<K, V>>).remove()
                    return true
                }
            }
            return false
        }

        override fun clear() = this@FilteredEntryMultimapView.clear()
    }

    override fun entries(): Collection<Map.Entry<K, V>> =
        filteredCollection(unfiltered.entries(), entryPredicate)

    override fun asMap(): Map<K, Collection<V>> = FilteredAsMap()

    override fun put(key: K, value: V): Boolean {
        require(satisfies(key, value)) { "Entry rejected by filter" }
        return unfiltered.put(key, value)
    }

    override fun putAll(key: K, values: Iterable<V>): Boolean {
        val materialized = values.toList()
        require(materialized.all { satisfies(key, it) }) { "Entry rejected by filter" }
        return unfiltered.putAll(key, materialized)
    }

    override fun putAll(multimap: Multimap<out K, out V>): Boolean {
        val materialized = multimap.entries().map { immutableEntry(it.key, it.value) }
        require(materialized.all(entryPredicate)) { "Entry rejected by filter" }
        var changed = false
        for (candidate in materialized) {
            if (unfiltered.put(candidate.key, candidate.value)) changed = true
        }
        return changed
    }

    override fun remove(key: Any?, value: Any?): Boolean {
        val candidate = unfiltered.entries().firstOrNull { it.key == key && it.value == value }
            ?: return false
        return satisfies(candidate.key, candidate.value) && unfiltered.remove(key, value)
    }

    override fun removeAll(key: Any?): Collection<V> {
        var found = false
        var matchedKey: Any? = null
        for (candidate in unfiltered.keySet()) {
            if (candidate == key) {
                found = true
                matchedKey = candidate
                break
            }
        }
        if (!found) return emptyList()
        @Suppress("UNCHECKED_CAST")
        val actualKey = matchedKey as K
        val collection = unfiltered.get(actualKey)
        val removed = if (collection is Set<*>) LinkedHashSet<V>() else ArrayList()
        val iterator = collection.iterator()
        while (iterator.hasNext()) {
            val value = iterator.next()
            if (satisfies(actualKey, value)) {
                iterator.remove()
                removed.add(value)
            }
        }
        return if (removed is Set<*>) {
            @Suppress("UNCHECKED_CAST")
            unmodifiableMutableSet(removed as Set<V>)
        } else {
            unmodifiableMutableList(removed.toList())
        }
    }

    override fun replaceValues(key: K, values: Iterable<V>): Collection<V> {
        val old = removeAll(key)
        putAll(key, values)
        return old
    }

    override fun clear() {
        val iterator = unfiltered.entries().iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (satisfies(candidate.key, candidate.value)) {
                @Suppress("UNCHECKED_CAST")
                (iterator as MutableIterator<Map.Entry<K, V>>).remove()
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other === this || (other is Multimap<*, *> && asMap() == other.asMap())

    override fun hashCode(): Int = asMap().hashCode()

    override fun toString(): String = asMap().toString()

    private inner class FilteredKeySet : AbstractMutableSet<K>() {
        override val size: Int get() =
            unfiltered.keySet().count { this@FilteredEntryMultimapView.containsKey(it) }

        override fun iterator(): MutableIterator<K> =
            filteringIterator(
                unfiltered.keySet().iterator(),
                { this@FilteredEntryMultimapView.containsKey(it) },
                allowRemove = false,
            )

        override fun contains(element: K): Boolean =
            this@FilteredEntryMultimapView.containsKey(element)

        override fun remove(element: K): Boolean =
            this@FilteredEntryMultimapView.removeAll(element).isNotEmpty()

        override fun clear() = this@FilteredEntryMultimapView.clear()

        override fun add(element: K): Nothing = throw UnsupportedOperationException()
    }

    private inner class FilteredKeysMultiset : AbstractMutableCollection<K>(), Multiset<K> {
        override val size: Int get() = this@FilteredEntryMultimapView.size()

        override fun count(element: Any?): Int =
            unfiltered.entries().count {
                it.key == element && satisfies(it.key, it.value)
            }

        override fun iterator(): MutableIterator<K> {
            val entries = this@FilteredEntryMultimapView.entries().iterator()
            return object : MutableIterator<K> {
                override fun hasNext(): Boolean = entries.hasNext()
                override fun next(): K = entries.next().key
                override fun remove(): Nothing = throw UnsupportedOperationException()
            }
        }

        override fun add(element: K): Nothing = throw UnsupportedOperationException()

        override fun add(element: K, occurrences: Int): Int = throw UnsupportedOperationException()

        override fun remove(element: Any?, occurrences: Int): Int {
            require(occurrences >= 0) { "occurrences cannot be negative: $occurrences" }
            val oldCount = count(element)
            if (occurrences == 0 || oldCount == 0) return oldCount
            var remaining = occurrences
            val iterator = unfiltered.entries().iterator()
            while (remaining > 0 && iterator.hasNext()) {
                val candidate = iterator.next()
                if (candidate.key == element && satisfies(candidate.key, candidate.value)) {
                    @Suppress("UNCHECKED_CAST")
                    (iterator as MutableIterator<Map.Entry<K, V>>).remove()
                    remaining--
                }
            }
            return oldCount
        }

        override fun setCount(element: K, count: Int): Int {
            require(count >= 0) { "count cannot be negative: $count" }
            val oldCount = count(element)
            if (count > oldCount) throw UnsupportedOperationException()
            remove(element, oldCount - count)
            return oldCount
        }

        override fun elementSet(): Set<K> = keySet()

        override fun entrySet(): Set<Multiset.Entry<K>> =
            keySet().mapTo(LinkedHashSet()) { key -> immutableMultisetEntry(key, count(key)) }

        override fun clear() = this@FilteredEntryMultimapView.clear()
    }

    private inner class FilteredAsMap : AbstractMutableMap<K, Collection<V>>() {
        override val size: Int get() = this@FilteredEntryMultimapView.keySet().size

        override fun containsKey(key: K): Boolean = this@FilteredEntryMultimapView.containsKey(key)

        override fun get(key: K): Collection<V>? =
            this@FilteredEntryMultimapView.get(key).takeIf { it.isNotEmpty() }

        override fun remove(key: K): Collection<V>? =
            this@FilteredEntryMultimapView.removeAll(key).takeIf { it.isNotEmpty() }

        override fun clear() = this@FilteredEntryMultimapView.clear()

        override fun put(key: K, value: Collection<V>): Collection<V>? =
            throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<K, Collection<V>>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, Collection<V>>>() {
                override val size: Int get() = this@FilteredAsMap.size

                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> {
                    val keys = this@FilteredEntryMultimapView.keySet().iterator()
                    return object : MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> {
                        override fun hasNext(): Boolean = keys.hasNext()

                        override fun next(): MutableMap.MutableEntry<K, Collection<V>> {
                            val key = keys.next()
                            return object : MutableMap.MutableEntry<K, Collection<V>> {
                                override val key: K = key
                                override val value: Collection<V>
                                    get() = this@FilteredAsMap.get(key)!!
                                override fun setValue(newValue: Collection<V>): Collection<V> =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor value.hashCode()
                                override fun toString(): String = "$key=$value"
                            }
                        }

                        override fun remove(): Nothing = throw UnsupportedOperationException()
                    }
                }

                override fun add(element: MutableMap.MutableEntry<K, Collection<V>>): Nothing =
                    throw UnsupportedOperationException()

                override fun remove(element: MutableMap.MutableEntry<K, Collection<V>>): Boolean {
                    val current = this@FilteredAsMap.get(element.key) ?: return false
                    if (current != element.value) return false
                    this@FilteredAsMap.remove(element.key)
                    return true
                }

                override fun clear() = this@FilteredAsMap.clear()
            }
    }
}

/** Set-preserving facade for Guava's SetMultimap filtering overloads. */
internal class FilteredEntrySetMultimapView<K, V>(
    internal val unfiltered: SetMultimap<K, V>,
    internal val entryPredicate: (Map.Entry<K, V>) -> Boolean,
) : SetMultimap<K, V> {
    private val delegate = FilteredEntryMultimapView(unfiltered, entryPredicate)

    override fun size(): Int = delegate.size()
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun containsKey(key: Any?): Boolean = delegate.containsKey(key)
    override fun containsValue(value: Any?): Boolean = delegate.containsValue(value)
    override fun containsEntry(key: Any?, value: Any?): Boolean = delegate.containsEntry(key, value)

    override fun get(key: K): MutableSet<V> {
        @Suppress("UNCHECKED_CAST")
        return delegate.get(key) as MutableSet<V>
    }

    override fun keySet(): Set<K> = delegate.keySet()
    override fun keys(): Multiset<K> = delegate.keys()
    override fun values(): Collection<V> = delegate.values()
    override fun entries(): Collection<Map.Entry<K, V>> = delegate.entries()

    override fun asMap(): Map<K, Set<V>> {
        @Suppress("UNCHECKED_CAST")
        return delegate.asMap() as Map<K, Set<V>>
    }

    override fun put(key: K, value: V): Boolean = delegate.put(key, value)
    override fun putAll(key: K, values: Iterable<V>): Boolean = delegate.putAll(key, values)
    override fun putAll(multimap: Multimap<out K, out V>): Boolean = delegate.putAll(multimap)
    override fun remove(key: Any?, value: Any?): Boolean = delegate.remove(key, value)

    override fun removeAll(key: Any?): Set<V> {
        val removed = delegate.removeAll(key)
        if (removed is Set<*>) {
            @Suppress("UNCHECKED_CAST")
            return removed as Set<V>
        }
        return unmodifiableMutableSet(removed.toSet())
    }

    override fun replaceValues(key: K, values: Iterable<V>): Set<V> {
        val removed = delegate.replaceValues(key, values)
        if (removed is Set<*>) {
            @Suppress("UNCHECKED_CAST")
            return removed as Set<V>
        }
        return unmodifiableMutableSet(removed.toSet())
    }

    override fun clear() = delegate.clear()
    override fun equals(other: Any?): Boolean = delegate == other
    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

/** List-preserving live view for Guava's key-filtering ListMultimap overload. */
internal class FilteredKeyListMultimapView<K, V>(
    internal val unfiltered: ListMultimap<K, V>,
    internal val keyPredicate: (K) -> Boolean,
) : ListMultimap<K, V> {
    private val delegate = FilteredEntryMultimapView(unfiltered) { keyPredicate(it.key) }

    override fun size(): Int = delegate.size()
    override fun isEmpty(): Boolean = delegate.isEmpty()
    override fun containsKey(key: Any?): Boolean = delegate.containsKey(key)
    override fun containsValue(value: Any?): Boolean = delegate.containsValue(value)
    override fun containsEntry(key: Any?, value: Any?): Boolean = delegate.containsEntry(key, value)

    override fun get(key: K): MutableList<V> =
        if (keyPredicate(key)) unfiltered.get(key) else AddRejectingEmptyList(key)

    override fun keySet(): Set<K> = delegate.keySet()
    override fun keys(): Multiset<K> = delegate.keys()
    override fun values(): Collection<V> = delegate.values()
    override fun entries(): Collection<Map.Entry<K, V>> = delegate.entries()
    override fun asMap(): Map<K, List<V>> = FilteredListAsMap()
    override fun put(key: K, value: V): Boolean = delegate.put(key, value)
    override fun putAll(key: K, values: Iterable<V>): Boolean = delegate.putAll(key, values)
    override fun putAll(multimap: Multimap<out K, out V>): Boolean = delegate.putAll(multimap)
    override fun remove(key: Any?, value: Any?): Boolean = delegate.remove(key, value)

    override fun removeAll(key: Any?): List<V> {
        val removed = delegate.removeAll(key)
        return if (removed is List<*>) {
            @Suppress("UNCHECKED_CAST")
            removed as List<V>
        } else {
            removed.toList()
        }
    }

    override fun replaceValues(key: K, values: Iterable<V>): List<V> {
        val materialized = values.toList()
        if (!keyPredicate(key)) {
            if (materialized.isEmpty()) return emptyList()
            throw IllegalArgumentException("Key rejected by filter: $key")
        }
        return unfiltered.replaceValues(key, materialized)
    }

    override fun clear() = delegate.clear()
    override fun equals(other: Any?): Boolean = delegate == other
    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()

    private inner class FilteredListAsMap : AbstractMutableMap<K, List<V>>() {
        override val size: Int get() = this@FilteredKeyListMultimapView.keySet().size
        override fun containsKey(key: K): Boolean =
            this@FilteredKeyListMultimapView.containsKey(key)
        override fun get(key: K): List<V>? =
            if (containsKey(key)) unfiltered.get(key) else null
        override fun remove(key: K): List<V>? =
            if (containsKey(key)) unfiltered.removeAll(key) else null
        override fun clear() = this@FilteredKeyListMultimapView.clear()
        override fun put(key: K, value: List<V>): List<V>? = throw UnsupportedOperationException()

        override val entries: MutableSet<MutableMap.MutableEntry<K, List<V>>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, List<V>>>() {
                override val size: Int get() = this@FilteredListAsMap.size

                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, List<V>>> {
                    val iterator = this@FilteredKeyListMultimapView.keySet().iterator()
                    return object : MutableIterator<MutableMap.MutableEntry<K, List<V>>> {
                        override fun hasNext(): Boolean = iterator.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, List<V>> {
                            val key = iterator.next()
                            return object : MutableMap.MutableEntry<K, List<V>> {
                                override val key: K = key
                                override val value: List<V> get() = unfiltered.get(key)
                                override fun setValue(newValue: List<V>): List<V> =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor value.hashCode()
                                override fun toString(): String = "$key=$value"
                            }
                        }

                        override fun remove(): Nothing = throw UnsupportedOperationException()
                    }
                }

                override fun add(element: MutableMap.MutableEntry<K, List<V>>): Nothing =
                    throw UnsupportedOperationException()

                override fun remove(element: MutableMap.MutableEntry<K, List<V>>): Boolean {
                    val current = this@FilteredListAsMap.get(element.key) ?: return false
                    if (current != element.value) return false
                    this@FilteredListAsMap.remove(element.key)
                    return true
                }

                override fun clear() = this@FilteredListAsMap.clear()
            }
    }
}

private class AddRejectingEmptyList<K, V>(
    private val key: K,
) : AbstractMutableList<V>() {
    override val size: Int get() = 0
    override fun get(index: Int): V = throw IndexOutOfBoundsException("index: $index, size: 0")

    override fun add(index: Int, element: V) {
        if (index != 0) throw IndexOutOfBoundsException("index: $index, size: 0")
        throw IllegalArgumentException("Key rejected by filter: $key")
    }

    override fun addAll(elements: Collection<V>): Boolean {
        throw IllegalArgumentException("Key rejected by filter: $key")
    }

    override fun addAll(index: Int, elements: Collection<V>): Boolean {
        if (index != 0) throw IndexOutOfBoundsException("index: $index, size: 0")
        throw IllegalArgumentException("Key rejected by filter: $key")
    }

    override fun removeAt(index: Int): V =
        throw IndexOutOfBoundsException("index: $index, size: 0")

    override fun set(index: Int, element: V): V =
        throw IndexOutOfBoundsException("index: $index, size: 0")
}

private fun <K, V> immutableEntry(key: K, value: V): Map.Entry<K, V> =
    object : Map.Entry<K, V> {
        override val key: K = key
        override val value: V = value

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)

        override fun toString(): String = "$key=$value"
    }

private fun <E> filteredCollection(
    backing: Collection<E>,
    predicate: (E) -> Boolean,
): MutableCollection<E> {
    @Suppress("UNCHECKED_CAST")
    val mutable = backing as MutableCollection<E>
    return if (backing is Set<*>) FilteredMutableSet(mutable as MutableSet<E>, predicate)
    else FilteredMutableCollection(mutable, predicate)
}

private open class FilteredMutableCollection<E>(
    protected val backing: MutableCollection<E>,
    private val predicate: (E) -> Boolean,
) : AbstractMutableCollection<E>() {
    override val size: Int get() = backing.count(predicate)

    override fun iterator(): MutableIterator<E> =
        filteringIterator(backing.iterator(), predicate, allowRemove = false)

    override fun contains(element: E): Boolean = predicate(element) && backing.contains(element)

    override fun add(element: E): Boolean {
        require(predicate(element)) { "Element rejected by filter" }
        return backing.add(element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        require(elements.all(predicate)) { "Element rejected by filter" }
        return backing.addAll(elements)
    }

    override fun remove(element: E): Boolean {
        val iterator = backing.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (candidate == element && predicate(candidate)) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    override fun removeAll(elements: Collection<E>): Boolean =
        removeMatching { predicate(it) && it in elements }

    override fun retainAll(elements: Collection<E>): Boolean =
        removeMatching { predicate(it) && it !in elements }

    override fun clear() {
        removeMatching(predicate)
    }

    private fun removeMatching(remove: (E) -> Boolean): Boolean {
        var changed = false
        val iterator = backing.iterator()
        while (iterator.hasNext()) {
            if (remove(iterator.next())) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }
}

private class FilteredMutableSet<E>(
    private val backing: MutableSet<E>,
    private val predicate: (E) -> Boolean,
) : AbstractMutableSet<E>() {
    override val size: Int get() = backing.count(predicate)

    override fun iterator(): MutableIterator<E> =
        filteringIterator(backing.iterator(), predicate, allowRemove = false)

    override fun contains(element: E): Boolean = predicate(element) && backing.contains(element)

    override fun add(element: E): Boolean {
        require(predicate(element)) { "Element rejected by filter" }
        return backing.add(element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        require(elements.all(predicate)) { "Element rejected by filter" }
        return backing.addAll(elements)
    }

    override fun remove(element: E): Boolean =
        predicate(element) && backing.remove(element)

    override fun removeAll(elements: Collection<E>): Boolean =
        removeMatching { predicate(it) && it in elements }

    override fun retainAll(elements: Collection<E>): Boolean =
        removeMatching { predicate(it) && it !in elements }

    override fun clear() {
        removeMatching(predicate)
    }

    private fun removeMatching(remove: (E) -> Boolean): Boolean {
        var changed = false
        val iterator = backing.iterator()
        while (iterator.hasNext()) {
            if (remove(iterator.next())) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }
}

private fun <E> filteringIterator(
    backing: Iterator<E>,
    predicate: (E) -> Boolean,
    allowRemove: Boolean,
): MutableIterator<E> = object : MutableIterator<E> {
    private var ready = false
    private var exhausted = false
    private var buffered: Any? = null

    override fun hasNext(): Boolean {
        if (ready) return true
        if (exhausted) return false
        while (backing.hasNext()) {
            val candidate = backing.next()
            if (predicate(candidate)) {
                buffered = candidate
                ready = true
                return true
            }
        }
        exhausted = true
        return false
    }

    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()
        ready = false
        @Suppress("UNCHECKED_CAST")
        return buffered as E
    }

    override fun remove() {
        if (!allowRemove) throw UnsupportedOperationException()
        @Suppress("UNCHECKED_CAST")
        (backing as MutableIterator<E>).remove()
    }
}

private fun <E> immutableMultisetEntry(element: E, count: Int): Multiset.Entry<E> =
    object : Multiset.Entry<E> {
        override fun getElement(): E = element
        override fun getCount(): Int = count

        override fun equals(other: Any?): Boolean =
            other is Multiset.Entry<*> && element == other.getElement() && count == other.getCount()

        override fun hashCode(): Int = (element?.hashCode() ?: 0) xor count
        override fun toString(): String = if (count == 1) "$element" else "$element x $count"
    }
