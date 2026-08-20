package com.bernaferrari.guavakt.collect

/**
 * A value equality and hash strategy that can be supplied to [EqualityMap] and [EqualitySet].
 *
 * Kotlin's [Any.equals] remains the default for ordinary collections. Use this type only when a
 * collection needs a different, explicit key contract such as ASCII-case-insensitive or deep
 * equality.
 */
interface Equality<in E> {
    fun equivalent(left: E, right: E): Boolean
    fun hash(value: E): Int

    companion object {
        fun <E> default(): Equality<E> = DefaultEquality()
        fun <E> identity(): Equality<E> = IdentityEquality()
    }
}

class DefaultEquality<E> : Equality<E> {
    override fun equivalent(left: E, right: E): Boolean = left == right
    override fun hash(value: E): Int = value?.hashCode() ?: 0
}

/** Referential equality. Distinct equal values remain distinct keys. */
class IdentityEquality<E> : Equality<E> {
    override fun equivalent(left: E, right: E): Boolean = left === right

    // Identity hashes are an optimization, not a correctness requirement: buckets compare with ===.
    override fun hash(value: E): Int = value?.hashCode() ?: 0
}

/** Equality of [E] values through a derived [F] value. */
class EqualityBy<E, F>(
    private val selector: (E) -> F,
    private val equality: Equality<F> = DefaultEquality(),
) : Equality<E> {
    override fun equivalent(left: E, right: E): Boolean = equality.equivalent(selector(left), selector(right))
    override fun hash(value: E): Int = equality.hash(selector(value))
}

/** ASCII-only case-insensitive string equality, appropriate for protocol tokens and header names. */
object CaseInsensitiveAsciiEquality : Equality<String> {
    override fun equivalent(left: String, right: String): Boolean {
        if (left.length != right.length) return false
        return left.indices.all { index ->
            val leftChar = left[index]
            val rightChar = right[index]
            leftChar == rightChar ||
                (leftChar in 'A'..'Z' || leftChar in 'a'..'z') &&
                (rightChar in 'A'..'Z' || rightChar in 'a'..'z') &&
                leftChar.lowercaseChar() == rightChar.lowercaseChar()
        }
    }

    override fun hash(value: String): Int {
        var result = 0
        for (char in value) {
            val normalized = if (char in 'A'..'Z') char.code + ('a'.code - 'A'.code) else char.code
            result = 31 * result + normalized
        }
        return result
    }
}

class IterableEquality<E>(private val elementEquality: Equality<E> = DefaultEquality()) : Equality<Iterable<E>> {
    override fun equivalent(left: Iterable<E>, right: Iterable<E>): Boolean {
        val leftIterator = left.iterator()
        val rightIterator = right.iterator()
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            if (!elementEquality.equivalent(leftIterator.next(), rightIterator.next())) return false
        }
        return !leftIterator.hasNext() && !rightIterator.hasNext()
    }

    override fun hash(value: Iterable<E>): Int {
        var result = 1
        for (element in value) result = 31 * result + elementEquality.hash(element)
        return result
    }
}

class ListEquality<E>(elementEquality: Equality<E> = DefaultEquality()) : Equality<List<E>> {
    private val iterableEquality = IterableEquality(elementEquality)
    override fun equivalent(left: List<E>, right: List<E>): Boolean =
        left.size == right.size && iterableEquality.equivalent(left, right)

    override fun hash(value: List<E>): Int = iterableEquality.hash(value)
}

class SetEquality<E>(private val elementEquality: Equality<E> = DefaultEquality()) : Equality<Set<E>> {
    override fun equivalent(left: Set<E>, right: Set<E>): Boolean {
        if (left.size != right.size) return false
        val remaining = EqualitySet(elementEquality).apply { addAll(right) }
        for (element in left) if (!remaining.remove(element)) return false
        return remaining.isEmpty()
    }

    override fun hash(value: Set<E>): Int = value.fold(0) { result, element -> result + elementEquality.hash(element) }
}

class MapEquality<K, V>(
    private val keyEquality: Equality<K> = DefaultEquality(),
    private val valueEquality: Equality<V> = DefaultEquality(),
) : Equality<Map<K, V>> {
    override fun equivalent(left: Map<K, V>, right: Map<K, V>): Boolean {
        if (left.size != right.size) return false
        val remaining = right.entries.toMutableList()
        for ((key, value) in left) {
            val index = remaining.indexOfFirst { entry ->
                keyEquality.equivalent(key, entry.key) && valueEquality.equivalent(value, entry.value)
            }
            if (index < 0) return false
            remaining.removeAt(index)
        }
        return true
    }

    override fun hash(value: Map<K, V>): Int = value.entries.fold(0) { result, (key, entryValue) ->
        result + (keyEquality.hash(key) xor valueEquality.hash(entryValue))
    }
}

/** Recursive equality for nested Kotlin [Map]s, [Set]s, and [Iterable]s. */
class DeepCollectionEquality(
    private val baseEquality: Equality<Any?> = DefaultEquality(),
) : Equality<Any?> {
    override fun equivalent(left: Any?, right: Any?): Boolean {
        if (left === right) return true
        if (left == null || right == null) return false
        return when {
            left is Map<*, *> && right is Map<*, *> -> mapsEquivalent(left, right)
            left is Set<*> && right is Set<*> -> setsEquivalent(left, right)
            left is Iterable<*> && right is Iterable<*> -> iterablesEquivalent(left, right)
            else -> baseEquality.equivalent(left, right)
        }
    }

    override fun hash(value: Any?): Int = when (value) {
        null -> 0
        is Map<*, *> -> value.entries.fold(0) { result, entry -> result + (hash(entry.key) xor hash(entry.value)) }
        is Set<*> -> value.fold(0) { result, element -> result + hash(element) }
        is Iterable<*> -> value.fold(1) { result, element -> 31 * result + hash(element) }
        else -> baseEquality.hash(value)
    }

    private fun iterablesEquivalent(left: Iterable<*>, right: Iterable<*>): Boolean {
        val leftIterator = left.iterator()
        val rightIterator = right.iterator()
        while (leftIterator.hasNext() && rightIterator.hasNext()) {
            if (!equivalent(leftIterator.next(), rightIterator.next())) return false
        }
        return !leftIterator.hasNext() && !rightIterator.hasNext()
    }

    private fun setsEquivalent(left: Set<*>, right: Set<*>): Boolean {
        if (left.size != right.size) return false
        val unmatched = right.toMutableList()
        for (element in left) {
            val index = unmatched.indexOfFirst { equivalent(element, it) }
            if (index < 0) return false
            unmatched.removeAt(index)
        }
        return true
    }

    private fun mapsEquivalent(left: Map<*, *>, right: Map<*, *>): Boolean {
        if (left.size != right.size) return false
        val unmatched = right.entries.toMutableList()
        for (entry in left.entries) {
            val index = unmatched.indexOfFirst { equivalent(entry.key, it.key) && equivalent(entry.value, it.value) }
            if (index < 0) return false
            unmatched.removeAt(index)
        }
        return true
    }
}

/** A mutable set whose membership is governed by an explicit [equality] strategy. */
class EqualitySet<E>(private val equality: Equality<E> = DefaultEquality()) : AbstractMutableSet<E>() {
    private val buckets = LinkedHashMap<Int, MutableList<E>>()

    override val size: Int get() = buckets.values.sumOf { it.size }

    override fun add(element: E): Boolean {
        val bucket = buckets.getOrPut(equality.hash(element)) { ArrayList() }
        if (bucket.any { equality.equivalent(it, element) }) return false
        bucket.add(element)
        return true
    }

    override fun contains(element: E): Boolean =
        buckets[equality.hash(element)]?.any { equality.equivalent(it, element) } == true

    override fun remove(element: E): Boolean {
        val hash = equality.hash(element)
        val bucket = buckets[hash] ?: return false
        val index = bucket.indexOfFirst { equality.equivalent(it, element) }
        if (index < 0) return false
        bucket.removeAt(index)
        if (bucket.isEmpty()) buckets.remove(hash)
        return true
    }

    override fun iterator(): MutableIterator<E> {
        val snapshot = buckets.values.flatten().toMutableList()
        val iterator = snapshot.iterator()
        var last: E? = null
        var canRemove = false
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next().also { last = it; canRemove = true }
            override fun remove() {
                check(canRemove) { "next() must be called before remove()" }
                @Suppress("UNCHECKED_CAST")
                this@EqualitySet.remove(last as E)
                iterator.remove()
                canRemove = false
            }
        }
    }

    override fun clear() = buckets.clear()
}

/** A mutable map whose key lookup is governed by an explicit [equality] strategy. */
class EqualityMap<K, V>(private val equality: Equality<K> = DefaultEquality()) : AbstractMutableMap<K, V>() {
    private data class StoredEntry<K, V>(val key: K, var value: V)
    private val buckets = LinkedHashMap<Int, MutableList<StoredEntry<K, V>>>()

    override val size: Int get() = buckets.values.sumOf { it.size }

    override fun get(key: K): V? = find(key)?.value
    override fun containsKey(key: K): Boolean = find(key) != null

    override fun put(key: K, value: V): V? {
        val hash = equality.hash(key)
        val bucket = buckets.getOrPut(hash) { ArrayList() }
        val entry = bucket.firstOrNull { equality.equivalent(it.key, key) }
        if (entry != null) {
            val oldValue = entry.value
            entry.value = value
            return oldValue
        }
        bucket.add(StoredEntry(key, value))
        return null
    }

    override fun remove(key: K): V? {
        val hash = equality.hash(key)
        val bucket = buckets[hash] ?: return null
        val index = bucket.indexOfFirst { equality.equivalent(it.key, key) }
        if (index < 0) return null
        val oldValue = bucket.removeAt(index).value
        if (bucket.isEmpty()) buckets.remove(hash)
        return oldValue
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = this@EqualityMap.size
        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean =
            this@EqualityMap.find(element.key)?.value == element.value && this@EqualityMap.containsKey(element.key)

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            if (!contains(element)) return false
            this@EqualityMap.remove(element.key)
            return true
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val snapshot = buckets.values.flatten().toMutableList()
            val iterator = snapshot.iterator()
            var last: StoredEntry<K, V>? = null
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableMap.MutableEntry<K, V> {
                    val stored = iterator.next()
                    last = stored
                    return object : MutableMap.MutableEntry<K, V> {
                        override val key: K get() = stored.key
                        override val value: V get() = stored.value
                        override fun setValue(newValue: V): V {
                            val oldValue = stored.value
                            stored.value = newValue
                            return oldValue
                        }

                        override fun equals(other: Any?): Boolean =
                            other is Map.Entry<*, *> && key == other.key && value == other.value

                        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
                    }
                }

                override fun remove() {
                    val stored = last ?: throw IllegalStateException("next() must be called before remove()")
                    this@EqualityMap.remove(stored.key)
                    iterator.remove()
                    last = null
                }
            }
        }

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            val present = this@EqualityMap.containsKey(element.key)
            this@EqualityMap[element.key] = element.value
            return !present
        }
    }

    override val keys: MutableSet<K> = object : AbstractMutableSet<K>() {
        override val size: Int get() = this@EqualityMap.size
        override fun contains(element: K): Boolean = this@EqualityMap.containsKey(element)
        override fun add(element: K): Boolean = throw UnsupportedOperationException("keys view does not support add")
        override fun remove(element: K): Boolean {
            if (!this@EqualityMap.containsKey(element)) return false
            this@EqualityMap.remove(element)
            return true
        }

        override fun iterator(): MutableIterator<K> {
            val iterator = this@EqualityMap.entries.iterator()
            return object : MutableIterator<K> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): K = iterator.next().key
                override fun remove() = iterator.remove()
            }
        }

        override fun clear() = this@EqualityMap.clear()
    }

    override fun clear() = buckets.clear()

    private fun find(key: K): StoredEntry<K, V>? =
        buckets[equality.hash(key)]?.firstOrNull { equality.equivalent(it.key, key) }
}
