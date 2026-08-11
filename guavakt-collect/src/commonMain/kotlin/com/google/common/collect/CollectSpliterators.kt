package dev.guavakt.collect

/** Guava CollectSpliterators — KMP uses Iterator; provides map/filter iterators. */
internal object CollectSpliterators {
    fun <F, T> map(from: Iterator<F>, function: (F) -> T): Iterator<T> =
        object : Iterator<T> {
            override fun hasNext(): Boolean = from.hasNext()
            override fun next(): T = function(from.next())
        }

    fun <T> filter(from: Iterator<T>, predicate: (T) -> Boolean): Iterator<T> =
        iterator {
            for (e in from) if (predicate(e)) yield(e)
        }

    fun <T> indexed(size: Int, function: (Int) -> T): Iterator<T> =
        (0 until size).iterator().let { idx ->
            object : Iterator<T> {
                override fun hasNext(): Boolean = idx.hasNext()
                override fun next(): T = function(idx.next())
            }
        }
}
