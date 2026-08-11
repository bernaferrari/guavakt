package dev.guavakt.util.concurrent

/** Guava AbstractFutureState — listener/value state holder used by AbstractFuture implementations. */
internal class AbstractFutureState<V> {
    @kotlin.concurrent.Volatile var value: Any? = null
    @kotlin.concurrent.Volatile var done = false
    val listeners = ArrayList<() -> Unit>()
}
