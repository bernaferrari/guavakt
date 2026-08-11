package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

/**
 * Guava LinkedListMultimap — global insertion order for [entries]/[values]/[keys],
 * plus per-key value order. Implemented with a doubly-linked node list (portable Kotlin).
 */
@GwtCompatible(serializable = true, emulated = true)
class LinkedListMultimap<K, V> private constructor() : ListMultimap<K, V> {
    private class Node<K, V>(
        override var key: K,
        override var value: V,
    ) : MutableMap.MutableEntry<K, V> {
        var next: Node<K, V>? = null
        var previous: Node<K, V>? = null
        var nextSibling: Node<K, V>? = null
        var previousSibling: Node<K, V>? = null
        override fun setValue(newValue: V): V {
            val old = value
            value = newValue
            return old
        }
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        override fun toString(): String = "$key=$value"
    }

    private class KeyList<K, V>(first: Node<K, V>) {
        var head: Node<K, V> = first
        var tail: Node<K, V> = first
        var count: Int = 1
    }

    private var head: Node<K, V>? = null
    private var tail: Node<K, V>? = null
    private val keyToKeyList = LinkedHashMap<K, KeyList<K, V>>()
    private var sizeField = 0

    override fun size(): Int = sizeField
    override fun isEmpty(): Boolean = sizeField == 0
    override fun containsKey(key: Any?): Boolean = keyToKeyList.containsKey(key)
    override fun containsValue(value: Any?): Boolean {
        var n = head
        while (n != null) {
            if (n.value == value) return true
            n = n.next
        }
        return false
    }
    override fun containsEntry(key: Any?, value: Any?): Boolean {
        var n = keyToKeyList[key]?.head
        while (n != null) {
            if (n.value == value) return true
            n = n.nextSibling
        }
        return false
    }

    override fun put(key: K, value: V): Boolean {
        addNode(key, value, null)
        return true
    }

    private fun addNode(key: K, value: V, nextSiblingHint: Node<K, V>?): Node<K, V> {
        val node = Node(key, value)
        if (head == null) {
            head = node
            tail = node
        } else {
            tail!!.next = node
            node.previous = tail
            tail = node
        }
        val keyList = keyToKeyList[key]
        if (keyList == null) {
            keyToKeyList[key] = KeyList(node)
        } else {
            keyList.count++
            if (nextSiblingHint == null) {
                keyList.tail.nextSibling = node
                node.previousSibling = keyList.tail
                keyList.tail = node
            } else {
                // insert before hint in sibling chain
                node.nextSibling = nextSiblingHint
                node.previousSibling = nextSiblingHint.previousSibling
                if (nextSiblingHint.previousSibling == null) keyList.head = node
                else nextSiblingHint.previousSibling!!.nextSibling = node
                nextSiblingHint.previousSibling = node
            }
        }
        sizeField++
        return node
    }

    private fun removeNode(node: Node<K, V>) {
        // Unlink from global entry list
        if (node.previous != null) node.previous!!.next = node.next else head = node.next
        if (node.next != null) node.next!!.previous = node.previous else tail = node.previous
        // Unlink from per-key sibling list
        val keyList = keyToKeyList[node.key] ?: run { sizeField--; return }
        if (node.previousSibling != null) {
            node.previousSibling!!.nextSibling = node.nextSibling
        } else if (node.nextSibling != null) {
            keyList.head = node.nextSibling!!
        }
        if (node.nextSibling != null) {
            node.nextSibling!!.previousSibling = node.previousSibling
        } else if (node.previousSibling != null) {
            keyList.tail = node.previousSibling!!
        }
        keyList.count--
        if (keyList.count == 0) keyToKeyList.remove(node.key)
        sizeField--
    }

    override fun remove(key: Any?, value: Any?): Boolean {
        var n = keyToKeyList[key]?.head
        while (n != null) {
            if (n.value == value) {
                removeNode(n)
                return true
            }
            n = n.nextSibling
        }
        return false
    }

    override fun removeAll(key: Any?): List<V> {
        val out = ArrayList<V>()
        var n = keyToKeyList[key]?.head
        while (n != null) {
            out.add(n.value)
            val next = n.nextSibling
            removeNode(n)
            n = next
        }
        return out
    }

    override fun replaceValues(key: K, values: Iterable<V>): List<V> {
        val old = removeAll(key)
        for (v in values) put(key, v)
        return old
    }

    override fun clear() {
        head = null
        tail = null
        keyToKeyList.clear()
        sizeField = 0
    }

    override fun get(key: K): MutableList<V> = object : AbstractMutableList<V>() {
        override val size: Int get() = keyToKeyList[key]?.count ?: 0
        override fun get(index: Int): V {
            var n = keyToKeyList[key]?.head ?: throw IndexOutOfBoundsException()
            var i = 0
            while (i < index) {
                n = n.nextSibling ?: throw IndexOutOfBoundsException()
                i++
            }
            return n.value
        }
        override fun add(index: Int, element: V) {
            if (index == size) {
                addNode(key, element, null)
                return
            }
            var n = keyToKeyList[key]?.head ?: run {
                Preconditions.checkArgument(index == 0)
                addNode(key, element, null)
                return
            }
            var i = 0
            while (i < index) {
                n = n.nextSibling ?: throw IndexOutOfBoundsException()
                i++
            }
            addNode(key, element, n)
        }
        override fun removeAt(index: Int): V {
            var n = keyToKeyList[key]?.head ?: throw IndexOutOfBoundsException()
            var i = 0
            while (i < index) {
                n = n.nextSibling ?: throw IndexOutOfBoundsException()
                i++
            }
            val v = n.value
            removeNode(n)
            return v
        }
        override fun set(index: Int, element: V): V {
            var n = keyToKeyList[key]?.head ?: throw IndexOutOfBoundsException()
            var i = 0
            while (i < index) {
                n = n.nextSibling ?: throw IndexOutOfBoundsException()
                i++
            }
            return n.setValue(element)
        }
    }

    override fun keys(): Multiset<K> {
        return object : AbstractMutableCollection<K>(), Multiset<K> {
            override val size: Int get() = sizeField
            override fun count(element: Any?): Int = keyToKeyList[element]?.count ?: 0
            override fun add(element: K): Boolean = throw UnsupportedOperationException("Use put")
            override fun add(element: K, occurrences: Int): Int = throw UnsupportedOperationException("Use put")
            override fun remove(element: Any?, occurrences: Int): Int {
                require(occurrences >= 0)
                val old = count(element)
                if (occurrences == 0 || old == 0) return old
                @Suppress("UNCHECKED_CAST")
                val key = element as K
                repeat(minOf(old, occurrences)) {
                    val node = keyToKeyList[key]?.head ?: return@repeat
                    removeNode(node)
                }
                return old
            }
            override fun setCount(element: K, count: Int): Int {
                require(count >= 0)
                val old = count(element)
                if (count > old) throw UnsupportedOperationException("Use put")
                remove(element, old - count)
                return old
            }
            override fun elementSet(): Set<K> = keySet()
            override fun entrySet(): Set<Multiset.Entry<K>> = keyToKeyList.map { (key, list) ->
                object : Multiset.Entry<K> {
                    override fun getElement(): K = key
                    override fun getCount(): Int = list.count
                }
            }.toSet()
            override fun iterator(): MutableIterator<K> {
                var next = head
                var last: Node<K, V>? = null
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean = next != null
                    override fun next(): K {
                        val node = next ?: throw NoSuchElementException()
                        next = node.next
                        last = node
                        return node.key
                    }
                    override fun remove() {
                        val node = last ?: throw IllegalStateException()
                        removeNode(node)
                        last = null
                    }
                }
            }
            override fun clear() = this@LinkedListMultimap.clear()
        }
    }

    /** Distinct keys in order of first remaining entry in the global list (Guava). */
    override fun keySet(): Set<K> {
        return object : AbstractMutableSet<K>() {
            override val size: Int get() = keyToKeyList.size
            override fun contains(element: K): Boolean = keyToKeyList.containsKey(element)
            override fun iterator(): MutableIterator<K> {
                val keys = LinkedHashSet<K>()
                var node = head
                while (node != null) {
                    keys.add(node.key)
                    node = node.next
                }
                val iterator = keys.iterator()
                var last: K? = null
                var canRemove = false
                return object : MutableIterator<K> {
                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): K = iterator.next().also { last = it; canRemove = true }
                    override fun remove() {
                        if (!canRemove) throw IllegalStateException()
                        removeAll(last)
                        canRemove = false
                    }
                }
            }
            override fun remove(element: K): Boolean {
                if (!keyToKeyList.containsKey(element)) return false
                removeAll(element)
                return true
            }
            override fun add(element: K): Boolean = throw UnsupportedOperationException()
            override fun clear() = this@LinkedListMultimap.clear()
        }
    }

    override fun values(): Collection<V> = object : AbstractMutableCollection<V>() {
        override val size: Int get() = sizeField
        override fun iterator(): MutableIterator<V> = object : MutableIterator<V> {
            private var n = head
            private var last: Node<K, V>? = null
            override fun hasNext() = n != null
            override fun next(): V {
                last = n ?: throw NoSuchElementException()
                n = n!!.next
                return last!!.value
            }
            override fun remove() {
                val c = last ?: throw IllegalStateException()
                removeNode(c)
                last = null
            }
        }
        override fun add(element: V) = throw UnsupportedOperationException()
    }

    override fun entries(): Collection<Map.Entry<K, V>> = object : AbstractMutableCollection<Map.Entry<K, V>>() {
        override val size: Int get() = sizeField
        override fun iterator(): MutableIterator<Map.Entry<K, V>> = object : MutableIterator<Map.Entry<K, V>> {
            private var n = head
            private var last: Node<K, V>? = null
            override fun hasNext() = n != null
            override fun next(): Map.Entry<K, V> {
                last = n ?: throw NoSuchElementException()
                n = n!!.next
                return last!!
            }
            override fun remove() {
                val c = last ?: throw IllegalStateException()
                removeNode(c)
                last = null
            }
        }
        override fun add(element: Map.Entry<K, V>) = throw UnsupportedOperationException()
    }

    override fun asMap(): Map<K, List<V>> = object : AbstractMutableMap<K, List<V>>() {
        override val entries: MutableSet<MutableMap.MutableEntry<K, List<V>>>
            get() = object : AbstractMutableSet<MutableMap.MutableEntry<K, List<V>>>() {
                override val size: Int get() = keyToKeyList.size
                override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, List<V>>> {
                    val keyIterator = this@LinkedListMultimap.keySet().iterator()
                    var lastKey: K? = null
                    var canRemove = false
                    return object : MutableIterator<MutableMap.MutableEntry<K, List<V>>> {
                        override fun hasNext(): Boolean = keyIterator.hasNext()
                        override fun next(): MutableMap.MutableEntry<K, List<V>> {
                            val key = keyIterator.next()
                            lastKey = key
                            canRemove = true
                            return object : MutableMap.MutableEntry<K, List<V>> {
                                override val key: K = key
                                override val value: List<V> get() = this@LinkedListMultimap.get(key)
                                override fun setValue(newValue: List<V>): List<V> =
                                    throw UnsupportedOperationException()
                                override fun equals(other: Any?): Boolean =
                                    other is Map.Entry<*, *> && key == other.key && value == other.value
                                override fun hashCode(): Int =
                                    (key?.hashCode() ?: 0) xor value.hashCode()
                                override fun toString(): String = "$key=$value"
                            }
                        }
                        override fun remove() {
                            if (!canRemove) throw IllegalStateException()
                            this@LinkedListMultimap.removeAll(lastKey)
                            canRemove = false
                        }
                    }
                }
                override fun add(element: MutableMap.MutableEntry<K, List<V>>): Boolean =
                    throw UnsupportedOperationException()
                override fun clear() = this@LinkedListMultimap.clear()
            }
        override fun get(key: K): List<V>? = if (containsKey(key)) this@LinkedListMultimap.get(key) else null
        override fun containsKey(key: K): Boolean = this@LinkedListMultimap.containsKey(key)
        override fun remove(key: K): List<V>? =
            if (containsKey(key)) this@LinkedListMultimap.removeAll(key) else null
        override fun put(key: K, value: List<V>): List<V>? = throw UnsupportedOperationException()
        override fun clear() = this@LinkedListMultimap.clear()
    }

    override fun putAll(key: K, values: Iterable<V>): Boolean {
        var changed = false
        for (v in values) if (put(key, v)) changed = true
        return changed
    }

    override fun putAll(multimap: Multimap<out K, out V>): Boolean {
        var changed = false
        for (e in multimap.entries()) if (put(e.key, e.value)) changed = true
        return changed
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Multimap<*, *>) return false
        return asMap() == other.asMap()
    }

    override fun hashCode(): Int = asMap().hashCode()

    override fun toString(): String = asMap().toString()

    companion object {
        fun <K, V> create(): LinkedListMultimap<K, V> = LinkedListMultimap()
        fun <K, V> create(multimap: Multimap<out K, out V>): LinkedListMultimap<K, V> =
            create<K, V>().also { it.putAll(multimap) }
        fun <K, V> create(expectedKeys: Int): LinkedListMultimap<K, V> = LinkedListMultimap()
    }
}
