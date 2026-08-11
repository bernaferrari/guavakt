package dev.guavakt.collect

/**
 * Guava FluentIterable — fluent wrappers over [Iterable] (stdlib-backed, Guava-shaped API).
 */
open class FluentIterable<E> protected constructor(
    private val iterable: Iterable<E>,
) : Iterable<E> {
    override fun iterator(): Iterator<E> = iterable.iterator()

    fun size(): Int = Iterables.size(iterable)
    fun isEmpty(): Boolean = Iterables.isEmpty(iterable)
    fun contains(target: Any?): Boolean = iterable.any { it == target }

    fun <T> transform(function: (E) -> T): FluentIterable<T> =
        FluentIterable(Iterables.transform(iterable, function))

    fun filter(predicate: (E) -> Boolean): FluentIterable<E> =
        FluentIterable(Iterables.filter(iterable, predicate))

    fun limit(maxSize: Int): FluentIterable<E> =
        FluentIterable(Iterables.limit(iterable, maxSize))

    fun skip(numberToSkip: Int): FluentIterable<E> =
        FluentIterable(Iterables.skip(iterable, numberToSkip))

    fun cycle(): FluentIterable<E> = FluentIterable(Iterables.cycle(iterable))

    fun append(other: Iterable<E>): FluentIterable<E> = FluentIterable(iterable + other)
    fun append(vararg elements: E): FluentIterable<E> = append(elements.asList())

    fun first(): E? = iterable.firstOrNull()
    fun last(): E? = iterable.lastOrNull()
    fun firstMatch(predicate: (E) -> Boolean): E? = iterable.firstOrNull(predicate)
    fun anyMatch(predicate: (E) -> Boolean): Boolean = iterable.any(predicate)
    fun allMatch(predicate: (E) -> Boolean): Boolean = iterable.all(predicate)

    fun get(position: Int): E = Iterables.get(iterable, position)
    fun get(position: Int, defaultValue: E): E = Iterables.get(iterable, position, defaultValue)

    fun toList(): List<E> = iterable.toList()
    fun toSet(): Set<E> = iterable.toSet()
    fun toSortedSet(comparator: Comparator<in E>): Set<E> =
        iterable.toMutableList().apply { sortWith(comparator) }.toSet()

    fun join(joiner: dev.guavakt.base.Joiner): String = joiner.join(iterable)

    fun <K> uniqueIndex(keyFunction: (E) -> K): Map<K, E> {
        val m = LinkedHashMap<K, E>()
        for (e in iterable) {
            val k = keyFunction(e)
            check(k !in m) { "Duplicate key: $k" }
            m[k] = e
        }
        return m
    }

    fun <V> toMap(valueFunction: (E) -> V): Map<E, V> {
        val m = LinkedHashMap<E, V>()
        for (e in iterable) m[e] = valueFunction(e)
        return m
    }

    fun copyInto(collection: MutableCollection<in E>): MutableCollection<in E> {
        collection.addAll(iterable.toList())
        return collection
    }

    override fun toString(): String = Iterables.toString(iterable)

    companion object {
        fun <E> from(iterable: Iterable<E>): FluentIterable<E> =
            if (iterable is FluentIterable) iterable else FluentIterable(iterable)

        fun <E> from(elements: Array<out E>): FluentIterable<E> = FluentIterable(elements.asList())

        fun <E> of(): FluentIterable<E> = FluentIterable(emptyList())

        fun <E> of(element: E, vararg elements: E): FluentIterable<E> =
            FluentIterable(listOf(element) + elements.toList())

        fun <T> concat(a: Iterable<T>, b: Iterable<T>): FluentIterable<T> = FluentIterable(a + b)

        fun <T> concat(inputs: Iterable<Iterable<T>>): FluentIterable<T> =
            FluentIterable(inputs.flatten())

        fun <T> concat(vararg inputs: Iterable<T>): FluentIterable<T> =
            FluentIterable(inputs.asList().flatten())
    }
}
