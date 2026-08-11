package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

@GwtCompatible(emulated = true)
object Iterators {
    fun <T> emptyIterator(): Iterator<T> = emptyList<T>().iterator()

    fun <T> emptyListIterator(): ListIterator<T> = emptyList<T>().listIterator()

    fun <T> singletonIterator(value: T): Iterator<T> = listOf(value).iterator()

    fun <T> forArray(vararg array: T): Iterator<T> = array.iterator()

    fun <T> forEnumeration(enumeration: Iterator<T>): Iterator<T> = enumeration

    fun <T> getNext(iterator: Iterator<T>, defaultValue: T): T =
        if (iterator.hasNext()) iterator.next() else defaultValue

    fun <T> getLast(iterator: Iterator<T>): T {
        Preconditions.checkArgument(iterator.hasNext())
        var current = iterator.next()
        while (iterator.hasNext()) current = iterator.next()
        return current
    }

    fun <T> getLast(iterator: Iterator<T>, defaultValue: T): T {
        return if (iterator.hasNext()) getLast(iterator) else defaultValue
    }

    fun <T> getOnlyElement(iterator: Iterator<T>): T {
        Preconditions.checkArgument(iterator.hasNext())
        val first = iterator.next()
        if (iterator.hasNext()) {
            val second = iterator.next()
            throw IllegalArgumentException("expected one element but was: <$first, $second>")
        }
        return first
    }

    fun <T> getOnlyElement(iterator: Iterator<T>, defaultValue: T): T {
        if (!iterator.hasNext()) return defaultValue
        return getOnlyElement(iterator)
    }

    fun <T> get(iterator: Iterator<T>, position: Int): T {
        Preconditions.checkArgument(position >= 0)
        var i = 0
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (i++ == position) return element
        }
        throw IndexOutOfBoundsException("position ($position) must be less than the number of elements that remained")
    }

    fun <T> get(iterator: Iterator<T>, position: Int, defaultValue: T): T {
        Preconditions.checkArgument(position >= 0)
        var i = 0
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (i++ == position) return element
        }
        return defaultValue
    }

    fun size(iterator: Iterator<*>): Int {
        var count = 0
        while (iterator.hasNext()) {
            iterator.next()
            count++
        }
        return count
    }

    fun <T> limit(iterator: Iterator<T>, limitSize: Int): Iterator<T> {
        Preconditions.checkArgument(limitSize >= 0)
        return object : Iterator<T> {
            private var count = 0
            override fun hasNext(): Boolean = count < limitSize && iterator.hasNext()
            override fun next(): T {
                if (!hasNext()) throw NoSuchElementException()
                count++
                return iterator.next()
            }
        }
    }

    fun <F, T> transform(fromIterator: Iterator<F>, function: (F) -> T): Iterator<T> =
        object : Iterator<T> {
            override fun hasNext(): Boolean = fromIterator.hasNext()
            override fun next(): T = function(fromIterator.next())
        }

    fun <T> filter(unfiltered: Iterator<T>, predicate: (T) -> Boolean): Iterator<T> =
        iterator {
            while (unfiltered.hasNext()) {
                val next = unfiltered.next()
                if (predicate(next)) yield(next)
            }
        }

    inline fun <reified T> filter(unfiltered: Iterator<*>): Iterator<T> =
        iterator {
            while (unfiltered.hasNext()) {
                val next = unfiltered.next()
                if (next is T) yield(next)
            }
        }

    fun <T> any(iterator: Iterator<T>, predicate: (T) -> Boolean): Boolean {
        while (iterator.hasNext()) if (predicate(iterator.next())) return true
        return false
    }

    fun <T> all(iterator: Iterator<T>, predicate: (T) -> Boolean): Boolean {
        while (iterator.hasNext()) if (!predicate(iterator.next())) return false
        return true
    }

    fun <T> find(iterator: Iterator<T>, predicate: (T) -> Boolean): T {
        while (iterator.hasNext()) {
            val t = iterator.next()
            if (predicate(t)) return t
        }
        throw NoSuchElementException()
    }

    fun <T> find(iterator: Iterator<T>, predicate: (T) -> Boolean, defaultValue: T): T {
        while (iterator.hasNext()) {
            val t = iterator.next()
            if (predicate(t)) return t
        }
        return defaultValue
    }

    fun <T : Any> tryFind(iterator: Iterator<T?>, predicate: (T?) -> Boolean): dev.guavakt.base.Optional<T> {
        while (iterator.hasNext()) {
            val t = iterator.next()
            if (predicate(t) && t != null) return dev.guavakt.base.Optional.of(t)
        }
        return dev.guavakt.base.Optional.absent()
    }

    fun <T> frequency(iterator: Iterator<T>, element: Any?): Int {
        var count = 0
        while (iterator.hasNext()) if (iterator.next() == element) count++
        return count
    }

    fun <T> cycle(iterable: Iterable<T>): Iterator<T> = Iterables.cycle(iterable).iterator()

    fun <T> cycle(vararg elements: T): Iterator<T> = cycle(elements.asList())

    fun <T> concat(vararg inputs: Iterator<T>): Iterator<T> = iterator {
        for (input in inputs) while (input.hasNext()) yield(input.next())
    }

    fun <T> concat(a: Iterator<T>, b: Iterator<T>): Iterator<T> = concat(a, b, emptyIterator())

    fun <T> concat(inputs: Iterator<out Iterator<T>>): Iterator<T> = iterator {
        while (inputs.hasNext()) {
            val it = inputs.next()
            while (it.hasNext()) yield(it.next())
        }
    }

    fun <T> addAll(addTo: MutableCollection<T>, iterator: Iterator<T>): Boolean {
        Preconditions.checkNotNull(addTo)
        Preconditions.checkNotNull(iterator)
        var wasModified = false
        while (iterator.hasNext()) wasModified = addTo.add(iterator.next()) || wasModified
        return wasModified
    }

    fun <T> elementsEqual(a: Iterator<*>, b: Iterator<*>): Boolean {
        while (a.hasNext()) {
            if (!b.hasNext()) return false
            if (a.next() != b.next()) return false
        }
        return !b.hasNext()
    }

    fun <T> toString(iterator: Iterator<T>): String {
        val sb = StringBuilder().append('[')
        var first = true
        while (iterator.hasNext()) {
            if (!first) sb.append(", ")
            first = false
            sb.append(iterator.next())
        }
        return sb.append(']').toString()
    }

    fun <T> toArray(iterator: Iterator<T>): List<T> {
        val list = ArrayList<T>()
        addAll(list, iterator)
        return list
    }

    fun <T> partition(iterator: Iterator<T>, size: Int): Iterator<List<T>> {
        Preconditions.checkArgument(size > 0)
        return iterator {
            while (iterator.hasNext()) {
                val chunk = ArrayList<T>(size)
                for (i in 0 until size) {
                    if (!iterator.hasNext()) break
                    chunk.add(iterator.next())
                }
                yield(chunk)
            }
        }
    }

    fun <T> paddedPartition(iterator: Iterator<T>, size: Int): Iterator<List<T?>> {
        Preconditions.checkArgument(size > 0)
        return iterator {
            while (iterator.hasNext()) {
                val chunk = ArrayList<T?>(size)
                for (i in 0 until size) {
                    if (iterator.hasNext()) chunk.add(iterator.next())
                    else chunk.add(null)
                }
                yield(chunk)
            }
        }
    }

    fun <T> advance(iterator: Iterator<T>, numberToAdvance: Int): Int {
        Preconditions.checkArgument(numberToAdvance >= 0)
        var i = 0
        while (i < numberToAdvance && iterator.hasNext()) {
            iterator.next()
            i++
        }
        return i
    }

    fun <T> removeAll(removeFrom: Iterator<T>, elementsToRemove: Collection<*>): Boolean {
        var modified = false
        // Iterator.remove not always available — only for MutableIterator
        if (removeFrom is MutableIterator) {
            while (removeFrom.hasNext()) {
                if (removeFrom.next() in elementsToRemove) {
                    removeFrom.remove()
                    modified = true
                }
            }
        }
        return modified
    }

    fun <T> retainAll(removeFrom: Iterator<T>, elementsToRetain: Collection<*>): Boolean {
        var modified = false
        if (removeFrom is MutableIterator) {
            while (removeFrom.hasNext()) {
                if (removeFrom.next() !in elementsToRetain) {
                    removeFrom.remove()
                    modified = true
                }
            }
        }
        return modified
    }

    fun <T> consumingIterator(iterator: Iterator<T>): Iterator<T> = iterator

    fun <T> peekingIterator(iterator: Iterator<T>): PeekingIterator<T> =
        if (iterator is PeekingIterator) iterator
        else PeekingImpl(iterator)

    fun <T> unmodifiableIterator(iterator: Iterator<T>): UnmodifiableIterator<T> =
        object : UnmodifiableIterator<T>() {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): T = iterator.next()
        }
}

private class PeekingImpl<E>(private val iterator: Iterator<E>) : PeekingIterator<E> {
    private var hasPeeked = false
    private var peekedElement: E? = null

    override fun hasNext(): Boolean = hasPeeked || iterator.hasNext()

    override fun next(): E {
        if (!hasPeeked) return iterator.next()
        @Suppress("UNCHECKED_CAST")
        val result = peekedElement as E
        hasPeeked = false
        peekedElement = null
        return result
    }

    override fun peek(): E {
        if (!hasPeeked) {
            peekedElement = iterator.next()
            hasPeeked = true
        }
        @Suppress("UNCHECKED_CAST")
        return peekedElement as E
    }
}
