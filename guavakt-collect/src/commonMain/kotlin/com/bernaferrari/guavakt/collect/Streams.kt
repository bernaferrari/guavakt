package com.bernaferrari.guavakt.collect

/**
 * Guava Streams utilities adapted to Kotlin [Sequence] / [Iterable] (no java.util.stream on KMP).
 */
object Streams {
    fun <T> stream(iterable: Iterable<T>): Sequence<T> = iterable.asSequence()

    fun <T> stream(iterator: Iterator<T>): Sequence<T> = iterator.asSequence()

    fun <A, B, R> zip(
        a: Iterable<A>,
        b: Iterable<B>,
        func: (A, B) -> R,
    ): Sequence<R> = sequence {
        val ia = a.iterator()
        val ib = b.iterator()
        while (ia.hasNext() && ib.hasNext()) {
            yield(func(ia.next(), ib.next()))
        }
    }

    /**
     * Lazily zips any number of iterables, stopping at the shortest input. Each emitted list has
     * one value from every input in the same position.
     */
    fun <T> zip(inputs: Iterable<Iterable<T>>): Sequence<List<T>> = sequence {
        val iterators = inputs.map { it.iterator() }
        while (iterators.all { it.hasNext() }) {
            yield(iterators.map { it.next() })
        }
    }

    fun <T> concat(vararg inputs: Iterable<T>): Sequence<T> = sequence {
        for (input in inputs) yieldAll(input)
    }

    fun <T> concat(inputs: Iterable<Iterable<T>>): Sequence<T> = sequence {
        for (input in inputs) yieldAll(input)
    }

    fun <T> findLast(iterable: Iterable<T>): T? {
        var last: T? = null
        var found = false
        for (e in iterable) {
            last = e
            found = true
        }
        return if (found) last else null
    }

    fun <T> mapWithIndex(iterable: Iterable<T>, func: (index: Int, T) -> T): Sequence<T> =
        iterable.asSequence().mapIndexed { i, t -> func(i, t) }

    fun <R> forEachPair(a: Iterable<*>, b: Iterable<*>, consumer: (Any?, Any?) -> R) {
        val ia = a.iterator()
        val ib = b.iterator()
        while (ia.hasNext() && ib.hasNext()) {
            consumer(ia.next(), ib.next())
        }
    }
}
