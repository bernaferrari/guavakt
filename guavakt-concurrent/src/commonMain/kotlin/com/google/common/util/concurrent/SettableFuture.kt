package dev.guavakt.util.concurrent

/**
 * Guava SettableFuture — publicly settable [AbstractFuture].
 */
class SettableFuture<V> private constructor() : AbstractFuture<V>() {
    fun set(value: V): Boolean = completeValue(value)
    fun setException(throwable: Throwable): Boolean = completeExceptionally(throwable)
    fun setFuture(future: ListenableFuture<out V>): Boolean = completeWithFuture(future)

    companion object {
        fun <V> create(): SettableFuture<V> = SettableFuture()
    }
}
