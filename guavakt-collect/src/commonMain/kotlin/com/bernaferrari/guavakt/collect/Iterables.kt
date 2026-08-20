package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.annotations.GwtCompatible
import com.bernaferrari.guavakt.base.Preconditions

/** Guava Iterables — via Kotlin stdlib (`map`/`filter`/`chunked`/…). Prefer collection extensions in new Kotlin. */
@GwtCompatible(emulated = true)
object Iterables {
    fun <T> getFirst(iterable: Iterable<T>, defaultValue: T): T {
        val it = iterable.iterator()
        return if (it.hasNext()) it.next() else defaultValue
    }

    fun <T> getLast(iterable: Iterable<T>): T {
        if (iterable is List) {
            Preconditions.checkArgument(iterable.isNotEmpty())
            return iterable[iterable.lastIndex]
        }
        val it = iterable.iterator()
        Preconditions.checkArgument(it.hasNext())
        var current = it.next()
        while (it.hasNext()) current = it.next()
        return current
    }

    fun <T> getLast(iterable: Iterable<T>, defaultValue: T): T {
        val it = iterable.iterator()
        if (!it.hasNext()) return defaultValue
        var current = it.next()
        while (it.hasNext()) current = it.next()
        return current
    }

    fun <T> getOnlyElement(iterable: Iterable<T>): T {
        val it = iterable.iterator()
        Preconditions.checkArgument(it.hasNext())
        val first = it.next()
        if (it.hasNext()) {
            val second = it.next()
            throw IllegalArgumentException("expected one element but was: <$first, $second>")
        }
        return first
    }

    fun <T> getOnlyElement(iterable: Iterable<T>, defaultValue: T): T {
        val it = iterable.iterator()
        if (!it.hasNext()) return defaultValue
        val first = it.next()
        if (it.hasNext()) {
            val second = it.next()
            throw IllegalArgumentException("expected one element but was: <$first, $second>")
        }
        return first
    }

    fun <T> get(iterable: Iterable<T>, position: Int): T {
        Preconditions.checkArgument(position >= 0)
        if (iterable is List<*>) {
            @Suppress("UNCHECKED_CAST")
            return (iterable as List<T>)[position]
        }
        val it = iterable.iterator()
        var i = 0
        while (it.hasNext()) {
            val element = it.next()
            if (i++ == position) return element
        }
        throw IndexOutOfBoundsException("position ($position) must be less than the number of elements that remained")
    }

    fun <T> get(iterable: Iterable<T>, position: Int, defaultValue: T): T {
        Preconditions.checkArgument(position >= 0)
        if (iterable is List<*>) {
            @Suppress("UNCHECKED_CAST")
            val list = iterable as List<T>
            return if (position < list.size) list[position] else defaultValue
        }
        val it = iterable.iterator()
        var i = 0
        while (it.hasNext()) {
            val element = it.next()
            if (i++ == position) return element
        }
        return defaultValue
    }

    fun <T> isEmpty(iterable: Iterable<T>): Boolean = !iterable.iterator().hasNext()

    fun size(iterable: Iterable<*>): Int =
        if (iterable is Collection) iterable.size else iterable.count()

    fun <T> concat(vararg inputs: Iterable<T>): Iterable<T> = inputs.asList().flatten()

    fun <T> concat(a: Iterable<T>, b: Iterable<T>): Iterable<T> = a + b

    fun <T> concat(a: Iterable<T>, b: Iterable<T>, c: Iterable<T>): Iterable<T> = a + b + c

    fun <T> concat(a: Iterable<T>, b: Iterable<T>, c: Iterable<T>, d: Iterable<T>): Iterable<T> =
        a + b + c + d

    fun <F, T> transform(fromIterable: Iterable<F>, function: (F) -> T): Iterable<T> =
        fromIterable.map(function)

    fun <T> filter(unfiltered: Iterable<T>, predicate: (T) -> Boolean): Iterable<T> =
        unfiltered.filter(predicate)

    inline fun <reified T> filter(unfiltered: Iterable<*>): Iterable<T> =
        unfiltered.filterIsInstance<T>()

    fun <T> any(iterable: Iterable<T>, predicate: (T) -> Boolean): Boolean = iterable.any(predicate)

    fun <T> all(iterable: Iterable<T>, predicate: (T) -> Boolean): Boolean = iterable.all(predicate)

    fun <T> find(iterable: Iterable<T>, predicate: (T) -> Boolean): T =
        iterable.first(predicate)

    fun <T> find(iterable: Iterable<T>, predicate: (T) -> Boolean, defaultValue: T): T =
        iterable.firstOrNull(predicate) ?: defaultValue

    fun <T : Any> tryFind(iterable: Iterable<T?>, predicate: (T?) -> Boolean): com.bernaferrari.guavakt.base.Optional<T> {
        for (t in iterable) {
            if (predicate(t) && t != null) return com.bernaferrari.guavakt.base.Optional.of(t)
        }
        return com.bernaferrari.guavakt.base.Optional.absent()
    }

    fun <T> frequency(iterable: Iterable<T>, element: Any?): Int = iterable.count { it == element }

    fun <T> limit(iterable: Iterable<T>, limitSize: Int): Iterable<T> {
        Preconditions.checkArgument(limitSize >= 0)
        return iterable.take(limitSize)
    }

    fun <T> skip(iterable: Iterable<T>, numberToSkip: Int): Iterable<T> {
        Preconditions.checkArgument(numberToSkip >= 0)
        return iterable.drop(numberToSkip)
    }

    /** Removes each element from a [MutableCollection] as it is iterated. */
    fun <T> consumingIterable(iterable: Iterable<T>): Iterable<T> = Iterable {
        if (iterable is MutableCollection<*>) {
            @Suppress("UNCHECKED_CAST")
            val col = iterable as MutableCollection<T>
            object : Iterator<T> {
                private val it = col.iterator()
                override fun hasNext() = it.hasNext()
                override fun next(): T {
                    val v = it.next()
                    it.remove()
                    return v
                }
            }
        } else {
            iterable.iterator()
        }
    }

    fun <T> cycle(iterable: Iterable<T>): Iterable<T> = Iterable {
        object : Iterator<T> {
            private var iterator = iterable.iterator()
            override fun hasNext(): Boolean {
                if (!iterator.hasNext()) {
                    iterator = iterable.iterator()
                    if (!iterator.hasNext()) return false
                }
                return true
            }
            override fun next(): T {
                if (!hasNext()) throw NoSuchElementException()
                return iterator.next()
            }
        }
    }

    fun <T> toArray(iterable: Iterable<T>): List<T> = iterable.toList()

    fun <T> toString(iterable: Iterable<T>): String = iterable.joinToString(prefix = "[", postfix = "]")

    fun <T> elementsEqual(a: Iterable<T>, b: Iterable<T>): Boolean {
        val ia = a.iterator()
        val ib = b.iterator()
        while (ia.hasNext() && ib.hasNext()) {
            if (ia.next() != ib.next()) return false
        }
        return !ia.hasNext() && !ib.hasNext()
    }

    fun <T> removeIf(removeFrom: Iterable<T>, predicate: (T) -> Boolean): Boolean {
        if (removeFrom is MutableCollection) return removeFrom.removeAll(predicate)
        throw UnsupportedOperationException()
    }

    fun <T> unmodifiableIterable(iterable: Iterable<T>): Iterable<T> = iterable.toList()

    fun <T> mergeSorted(
        iterables: Iterable<out Iterable<T>>,
        comparator: Comparator<in T>,
    ): Iterable<T> {
        val lists = iterables.map { it.toList() }
        return lists.flatten().sortedWith(comparator)
    }

    fun <T> partition(iterable: Iterable<T>, size: Int): Iterable<List<T>> {
        Preconditions.checkArgument(size > 0)
        return iterable.chunked(size)
    }

    fun <T> paddedPartition(iterable: Iterable<T>, size: Int): Iterable<List<T?>> {
        Preconditions.checkArgument(size > 0)
        val chunks = iterable.chunked(size)
        return chunks.map { chunk ->
            if (chunk.size == size) chunk
            else chunk + List(size - chunk.size) { null }
        }
    }

    /** Lazily splits [iterable] immediately after every element that satisfies [predicate]. */
    fun <T> splitAfter(iterable: Iterable<T>, predicate: (T) -> Boolean): Iterable<List<T>> =
        splitWhen(iterable, shouldSplitAfter = { _, current, _ -> predicate(current) })

    /** Lazily splits [iterable] immediately before every element that satisfies [predicate]. */
    fun <T> splitBefore(iterable: Iterable<T>, predicate: (T) -> Boolean): Iterable<List<T>> =
        splitWhen(iterable, shouldSplitBefore = { hasPrevious, _, current, _ -> hasPrevious && predicate(current) })

    /** Lazily splits [iterable] between adjacent elements when [predicate] returns true. */
    fun <T> splitBetween(iterable: Iterable<T>, predicate: (T, T) -> Boolean): Iterable<List<T>> =
        splitWhen(iterable, shouldSplitBefore = { hasPrevious, previous, current, _ ->
            if (!hasPrevious) {
                false
            } else {
                @Suppress("UNCHECKED_CAST")
                predicate(previous as T, current)
            }
        })

    private fun <T> splitWhen(
        iterable: Iterable<T>,
        shouldSplitBefore: (hasPrevious: Boolean, previous: T?, current: T, index: Int) -> Boolean = { _, _, _, _ -> false },
        shouldSplitAfter: (hasPrevious: Boolean, current: T, index: Int) -> Boolean = { _, _, _ -> false },
    ): Iterable<List<T>> = Iterable {
        val source = iterable.iterator()
        object : Iterator<List<T>> {
            private var buffered: T? = null
            private var hasBuffered = false
            private var previous: T? = null
            private var hasPrevious = false
            private var index = 0

            override fun hasNext(): Boolean = hasBuffered || source.hasNext()

            override fun next(): List<T> {
                if (!hasNext()) throw NoSuchElementException()
                val result = ArrayList<T>()
                while (hasBuffered || source.hasNext()) {
                    val current = if (hasBuffered) {
                        hasBuffered = false
                        @Suppress("UNCHECKED_CAST")
                        buffered as T
                    } else {
                        source.next()
                    }
                    if (result.isNotEmpty() && shouldSplitBefore(hasPrevious, previous, current, index)) {
                        buffered = current
                        hasBuffered = true
                        break
                    }
                    result.add(current)
                    previous = current
                    hasPrevious = true
                    index++
                    if (shouldSplitAfter(hasPrevious, current, index - 1)) break
                }
                return result
            }
        }
    }
}
