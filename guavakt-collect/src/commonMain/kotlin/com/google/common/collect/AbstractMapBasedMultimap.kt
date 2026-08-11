package dev.guavakt.collect

import dev.guavakt.base.Preconditions

/**
 * Guava [AbstractMapBasedMultimap]: map from key → collection of values, with **live views**
 * for [get], [keySet], [values], [entries], and [asMap] (mutations through views update the multimap).
 */
abstract class AbstractMapBasedMultimap<K, V> protected constructor(
    private val map: MutableMap<K, MutableCollection<V>>,
) : Multimap<K, V> {

    private var totalSize: Int = 0

    init {
        Preconditions.checkArgument(map.isEmpty())
    }

    /** New empty collection for a key (ArrayList, HashSet, …). */
    protected abstract fun createCollection(): MutableCollection<V>

    protected open fun createCollection(key: K): MutableCollection<V> = createCollection()

    protected fun backingMap(): MutableMap<K, MutableCollection<V>> = map

    protected fun totalSize(): Int = totalSize

    protected fun setTotalSize(size: Int) {
        totalSize = size
    }

    override fun size(): Int = totalSize

    override fun isEmpty(): Boolean = totalSize == 0

    override fun containsKey(key: Any?): Boolean = map.containsKey(key)

    override fun containsValue(value: Any?): Boolean {
        for (c in map.values) if (c.contains(value)) return true
        return false
    }

    override fun containsEntry(key: Any?, value: Any?): Boolean {
        val c = map[key] ?: return false
        return c.contains(value)
    }

    override fun get(key: K): MutableCollection<V> = wrapCollection(key, map[key])

    private fun wrapCollection(key: K, delegate: MutableCollection<V>?): MutableCollection<V> {
        @Suppress("UNCHECKED_CAST")
        return when (val created = createCollection()) {
            is MutableList<*> -> WrappedList(key, delegate as MutableList<V>?, this as AbstractMapBasedMultimap<K, V>)
            is MutableSet<*> -> WrappedSet(key, delegate as MutableSet<V>?, this as AbstractMapBasedMultimap<K, V>)
            else -> WrappedCollection(key, delegate, this)
        }
    }

    override fun keySet(): Set<K> = KeySet()

    override fun keys(): Multiset<K> = KeysMultiset()

    /** Live multiset view of keys (count = size of value collection). Prefer multimap.put for growth. */
    private inner class KeysMultiset : AbstractMutableCollection<K>(), Multiset<K> {
        override val size: Int get() = totalSize()
        override fun count(element: Any?): Int {
            @Suppress("UNCHECKED_CAST")
            return map[element as? K]?.size ?: 0
        }
        override fun add(element: K, occurrences: Int): Int =
            throw UnsupportedOperationException("Use Multimap.put to add values")
        override fun add(element: K): Boolean =
            throw UnsupportedOperationException("Use Multimap.put to add values")
        override fun remove(element: Any?, occurrences: Int): Int {
            require(occurrences >= 0)
            val col = map[element] ?: return 0
            val old = col.size
            var left = minOf(occurrences, old)
            val it = col.iterator()
            while (left > 0 && it.hasNext()) {
                it.next(); it.remove(); setTotalSize(totalSize() - 1); left--
            }
            if (col.isEmpty()) map.remove(element)
            return old
        }
        override fun setCount(element: K, count: Int): Int {
            require(count >= 0)
            val old = count(element)
            when {
                count == old -> {}
                count == 0 -> removeAll(element)
                count < old -> remove(element, old - count)
                else -> throw UnsupportedOperationException("Use Multimap.put to add values")
            }
            return old
        }
        override fun elementSet(): Set<K> = keySet()
        override fun entrySet(): Set<Multiset.Entry<K>> =
            map.map { (k, col) ->
                object : Multiset.Entry<K> {
                    override fun getElement(): K = k
                    override fun getCount(): Int = col.size
                    override fun equals(other: Any?): Boolean =
                        other is Multiset.Entry<*> && k == other.getElement() && col.size == other.getCount()
                    override fun hashCode(): Int = (k?.hashCode() ?: 0) xor col.size
                    override fun toString(): String = if (col.size == 1) "$k" else "$k x ${col.size}"
                }
            }.toSet()
        override fun iterator(): MutableIterator<K> {
            val flat = ArrayList<K>(totalSize())
            for ((k, col) in map) repeat(col.size) { flat.add(k) }
            val it = flat.listIterator()
            return object : MutableIterator<K> {
                private var last: K? = null
                private var canRemove = false
                override fun hasNext() = it.hasNext()
                override fun next(): K = it.next().also { last = it; canRemove = true }
                override fun remove() {
                    if (!canRemove) throw IllegalStateException()
                    this@KeysMultiset.remove(last, 1)
                    it.remove()
                    canRemove = false
                }
            }
        }
        override fun clear() = this@AbstractMapBasedMultimap.clear()
        override fun equals(other: Any?): Boolean = Multisets.equalsImpl(this, other)
        override fun hashCode(): Int = entrySet().sumOf { it.hashCode() }
        override fun toString(): String = entrySet().toString()
    }

    override fun values(): Collection<V> = Values()

    override fun entries(): Collection<Map.Entry<K, V>> = Entries()

    override fun asMap(): Map<K, Collection<V>> = AsMap()

    override fun put(key: K, value: V): Boolean {
        var collection = map[key]
        if (collection == null) {
            collection = createCollection(key)
            if (collection.add(value)) {
                totalSize++
                map[key] = collection
                return true
            } else {
                throw AssertionError("New collection violated the Collection contract")
            }
        } else if (collection.add(value)) {
            totalSize++
            return true
        }
        return false
    }

    override fun putAll(key: K, values: Iterable<V>): Boolean {
        val iterator = values.iterator()
        if (!iterator.hasNext()) return false
        var collection = map[key]
        if (collection == null) {
            collection = createCollection(key)
            var changed = false
            while (iterator.hasNext()) {
                if (collection.add(iterator.next())) {
                    totalSize++
                    changed = true
                }
            }
            if (changed) map[key] = collection
            return changed
        }
        var changed = false
        while (iterator.hasNext()) {
            if (collection.add(iterator.next())) {
                totalSize++
                changed = true
            }
        }
        return changed
    }

    override fun putAll(multimap: Multimap<out K, out V>): Boolean {
        var changed = false
        for (entry in multimap.entries()) {
            if (put(entry.key, entry.value)) changed = true
        }
        return changed
    }

    override fun remove(key: Any?, value: Any?): Boolean {
        val collection = map[key] ?: return false
        val changed = collection.remove(value)
        if (changed) {
            totalSize--
            if (collection.isEmpty()) map.remove(key)
        }
        return changed
    }

    override fun removeAll(key: Any?): Collection<V> {
        val collection = map.remove(key) ?: return unmodifiableEmpty()
        totalSize -= collection.size
        val result = createCollection()
        result.addAll(collection)
        collection.clear()
        return unmodifiable(result)
    }

    override fun replaceValues(key: K, values: Iterable<V>): Collection<V> {
        val result = removeAll(key)
        putAll(key, values)
        return result
    }

    override fun clear() {
        for (c in map.values) c.clear()
        map.clear()
        totalSize = 0
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Multimap<*, *>) return false
        return asMap() == other.asMap()
    }

    override fun hashCode(): Int = asMap().hashCode()

    override fun toString(): String = asMap().toString()

    protected open fun unmodifiableEmpty(): Collection<V> = unmodifiable(createCollection())

    protected open fun unmodifiable(collection: Collection<V>): Collection<V> =
        when (collection) {
            is List<*> -> collection.toList()
            is Set<*> -> collection.toSet()
            else -> collection.toList()
        }

    // --- live views ---

    private inner class KeySet : AbstractMutableSet<K>() {
        override val size: Int get() = map.size
        override fun iterator(): MutableIterator<K> = object : MutableIterator<K> {
            private val it = map.entries.iterator()
            private var current: MutableMap.MutableEntry<K, MutableCollection<V>>? = null
            override fun hasNext(): Boolean = it.hasNext()
            override fun next(): K {
                current = it.next()
                return current!!.key
            }
            override fun remove() {
                val entry = current ?: throw IllegalStateException()
                totalSize -= entry.value.size
                entry.value.clear()
                it.remove()
                current = null
            }
        }
        override fun contains(element: K): Boolean = map.containsKey(element)
        override fun remove(element: K): Boolean {
            val c = map.remove(element) ?: return false
            totalSize -= c.size
            c.clear()
            return true
        }
        override fun clear() = this@AbstractMapBasedMultimap.clear()
        override fun add(element: K): Boolean = throw UnsupportedOperationException()
    }

    private inner class Values : AbstractMutableCollection<V>() {
        override val size: Int get() = totalSize
        override fun iterator(): MutableIterator<V> = object : MutableIterator<V> {
            private val keyIt = map.entries.iterator()
            private var valueIt: MutableIterator<V>? = null
            private var currentCollection: MutableCollection<V>? = null
            private var removeFrom: MutableCollection<V>? = null
            override fun hasNext(): Boolean {
                while (true) {
                    val vit = valueIt
                    if (vit != null && vit.hasNext()) return true
                    if (!keyIt.hasNext()) return false
                    val e = keyIt.next()
                    currentCollection = e.value
                    valueIt = e.value.iterator()
                }
            }
            override fun next(): V {
                if (!hasNext()) throw NoSuchElementException()
                removeFrom = currentCollection
                return valueIt!!.next()
            }
            override fun remove() {
                val col = removeFrom ?: throw IllegalStateException()
                valueIt!!.remove()
                totalSize--
                if (col.isEmpty()) keyIt.remove()
                removeFrom = null
            }
        }
        override fun clear() = this@AbstractMapBasedMultimap.clear()
        override fun add(element: V): Boolean = throw UnsupportedOperationException()
    }

    private inner class Entries : AbstractMutableCollection<Map.Entry<K, V>>() {
        override val size: Int get() = totalSize
        override fun iterator(): MutableIterator<Map.Entry<K, V>> = object : MutableIterator<Map.Entry<K, V>> {
            private val keyIt = map.entries.iterator()
            private var valueIt: MutableIterator<V>? = null
            private var currentKey: K? = null
            private var currentCollection: MutableCollection<V>? = null
            private var canRemove = false
            override fun hasNext(): Boolean {
                while (true) {
                    val vit = valueIt
                    if (vit != null && vit.hasNext()) return true
                    if (!keyIt.hasNext()) return false
                    val e = keyIt.next()
                    currentKey = e.key
                    currentCollection = e.value
                    valueIt = e.value.iterator()
                }
            }
            override fun next(): Map.Entry<K, V> {
                if (!hasNext()) throw NoSuchElementException()
                canRemove = true
                val k = currentKey as K
                val v = valueIt!!.next()
                return object : Map.Entry<K, V> {
                    override val key: K get() = k
                    override val value: V get() = v
                    override fun equals(other: Any?): Boolean =
                        other is Map.Entry<*, *> && k == other.key && v == other.value
                    override fun hashCode(): Int = (k?.hashCode() ?: 0) xor (v?.hashCode() ?: 0)
                    override fun toString(): String = "$k=$v"
                }
            }
            override fun remove() {
                check(canRemove)
                valueIt!!.remove()
                totalSize--
                canRemove = false
                val col = currentCollection!!
                if (col.isEmpty()) keyIt.remove()
            }
        }
        override fun clear() = this@AbstractMapBasedMultimap.clear()
        override fun add(element: Map.Entry<K, V>): Boolean = throw UnsupportedOperationException()
    }

    private inner class AsMap : AbstractMutableMap<K, Collection<V>>() {
        override val entries: MutableSet<MutableMap.MutableEntry<K, Collection<V>>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, Collection<V>>>() {
                override val size: Int get() = map.size
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> =
                    object : MutableIterator<MutableMap.MutableEntry<K, Collection<V>>> {
                        private val it = map.entries.iterator()
                        private var last: MutableMap.MutableEntry<K, MutableCollection<V>>? = null
                        override fun hasNext(): Boolean = it.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, Collection<V>> {
                            val e = it.next()
                            last = e
                            val k = e.key
                            return object : MutableMap.MutableEntry<K, Collection<V>> {
                                override val key: K get() = k
                                override var value: Collection<V>
                                    get() = wrapCollection(k, map[k])
                                    set(_) = throw UnsupportedOperationException()
                                override fun setValue(newValue: Collection<V>): Collection<V> =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor value.hashCode()
                                override fun toString(): String = "$key=$value"
                            }
                        }
                        override fun remove() {
                            val e = last ?: throw IllegalStateException()
                            totalSize -= e.value.size
                            e.value.clear()
                            it.remove()
                            last = null
                        }
                    }
                override fun add(element: MutableMap.MutableEntry<K, Collection<V>>): Boolean =
                    throw UnsupportedOperationException()
                override fun clear() = this@AbstractMapBasedMultimap.clear()
            }
        override fun get(key: K): Collection<V>? =
            if (map.containsKey(key)) wrapCollection(key, map[key]) else null
        override fun containsKey(key: K): Boolean = map.containsKey(key)
        override fun remove(key: K): Collection<V>? {
            val c = map.remove(key) ?: return null
            totalSize -= c.size
            val result = createCollection()
            result.addAll(c)
            c.clear()
            return unmodifiable(result)
        }
        override fun clear() = this@AbstractMapBasedMultimap.clear()
        override fun put(key: K, value: Collection<V>): Collection<V>? =
            throw UnsupportedOperationException()
    }

    private open class WrappedCollection<K, V>(
        protected val key: K,
        protected var delegate: MutableCollection<V>?,
        protected val multimap: AbstractMapBasedMultimap<K, V>,
    ) : AbstractMutableCollection<V>() {
        protected fun refreshIfEmpty() {
            if (delegate == null || delegate!!.isEmpty()) {
                val newDelegate = multimap.map[key]
                if (newDelegate != null) delegate = newDelegate
            }
        }

        protected fun addToMap() {
            if (delegate == null) {
                delegate = multimap.createCollection(key)
            }
            if (multimap.map[key] !== delegate) multimap.map[key] = delegate!!
        }

        override val size: Int
            get() {
                refreshIfEmpty()
                return delegate?.size ?: 0
            }

        override fun iterator(): MutableIterator<V> {
            refreshIfEmpty()
            val d = delegate
            if (d == null) {
                return object : MutableIterator<V> {
                    override fun hasNext(): Boolean = false
                    override fun next(): V = throw NoSuchElementException()
                    override fun remove() = throw IllegalStateException()
                }
            }
            val it = d.iterator()
            return object : MutableIterator<V> {
                override fun hasNext(): Boolean = it.hasNext()
                override fun next(): V = it.next()
                override fun remove() {
                    it.remove()
                    multimap.totalSize--
                    removeIfEmpty()
                }
            }
        }

        override fun add(element: V): Boolean {
            refreshIfEmpty()
            val wasEmpty = delegate == null || delegate!!.isEmpty()
            addToMap()
            val changed = delegate!!.add(element)
            if (changed) {
                multimap.totalSize++
                if (wasEmpty) {
                    // already in map via addToMap
                }
            }
            return changed
        }

        override fun clear() {
            refreshIfEmpty()
            val d = delegate ?: return
            multimap.totalSize -= d.size
            d.clear()
            removeIfEmpty()
        }

        protected fun removeIfEmpty() {
            val d = delegate
            if (d != null && d.isEmpty()) {
                multimap.map.remove(key)
                delegate = null
            }
        }
    }

    private class WrappedList<K, V>(
        key: K,
        delegate: MutableList<V>?,
        multimap: AbstractMapBasedMultimap<K, V>,
    ) : WrappedCollection<K, V>(key, delegate, multimap), MutableList<V> {

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is List<*> || other.size != size) return false
            return indices.all { this[it] == other[it] }
        }

        override fun hashCode(): Int {
            var hash = 1
            for (element in this) hash = 31 * hash + (element?.hashCode() ?: 0)
            return hash
        }

        private fun listDelegate(): MutableList<V> {
            refreshIfEmpty()
            @Suppress("UNCHECKED_CAST")
            return (delegate as MutableList<V>?) ?: run {
                delegate = multimap.createCollection(key)
                delegate as MutableList<V>
            }
        }

        override fun get(index: Int): V = listDelegate()[index]

        override fun set(index: Int, element: V): V = listDelegate().set(index, element)

        override fun add(index: Int, element: V) {
            refreshIfEmpty()
            val wasEmpty = delegate == null || delegate!!.isEmpty()
            addToMap()
            (delegate as MutableList<V>).add(index, element)
            multimap.totalSize++
        }

        override fun removeAt(index: Int): V {
            val removed = listDelegate().removeAt(index)
            multimap.totalSize--
            removeIfEmpty()
            return removed
        }

        override fun addAll(index: Int, elements: Collection<V>): Boolean {
            if (elements.isEmpty()) return false
            refreshIfEmpty()
            addToMap()
            val list = delegate as MutableList<V>
            val changed = list.addAll(index, elements)
            if (changed) multimap.totalSize += elements.size
            return changed
        }

        override fun indexOf(element: V): Int {
            refreshIfEmpty()
            @Suppress("UNCHECKED_CAST")
            return (delegate as MutableList<V>?)?.indexOf(element) ?: -1
        }

        override fun lastIndexOf(element: V): Int {
            refreshIfEmpty()
            @Suppress("UNCHECKED_CAST")
            return (delegate as MutableList<V>?)?.lastIndexOf(element) ?: -1
        }

        override fun listIterator(): MutableListIterator<V> = listIterator(0)

        override fun listIterator(index: Int): MutableListIterator<V> {
            refreshIfEmpty()
            val list = listDelegate()
            val it = list.listIterator(index)
            return object : MutableListIterator<V> by it {
                override fun add(element: V) {
                    it.add(element)
                    if (multimap.map[key] !== list) multimap.map[key] = list
                    multimap.totalSize++
                }
                override fun remove() {
                    it.remove()
                    multimap.totalSize--
                    removeIfEmpty()
                }
            }
        }

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<V> {
            if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
                throw IndexOutOfBoundsException("fromIndex=$fromIndex, toIndex=$toIndex, size=$size")
            }
            val parent = this
            return object : AbstractMutableList<V>() {
                private var length = toIndex - fromIndex
                override val size: Int get() = length
                override fun get(index: Int): V {
                    checkElementIndex(index)
                    return parent[fromIndex + index]
                }
                override fun set(index: Int, element: V): V {
                    checkElementIndex(index)
                    return parent.set(fromIndex + index, element)
                }
                override fun add(index: Int, element: V) {
                    if (index < 0 || index > length) throw IndexOutOfBoundsException()
                    parent.add(fromIndex + index, element)
                    length++
                }
                override fun removeAt(index: Int): V {
                    checkElementIndex(index)
                    val removed = parent.removeAt(fromIndex + index)
                    length--
                    return removed
                }
                private fun checkElementIndex(index: Int) {
                    if (index < 0 || index >= length) throw IndexOutOfBoundsException()
                }
            }
        }
    }

    private class WrappedSet<K, V>(
        key: K,
        delegate: MutableSet<V>?,
        multimap: AbstractMapBasedMultimap<K, V>,
    ) : WrappedCollection<K, V>(key, delegate, multimap), MutableSet<V> {
        override fun equals(other: Any?): Boolean =
            other === this || (other is Set<*> && other.size == size && all { it in other })

        override fun hashCode(): Int = fold(0) { hash, element -> hash + (element?.hashCode() ?: 0) }
    }
}
