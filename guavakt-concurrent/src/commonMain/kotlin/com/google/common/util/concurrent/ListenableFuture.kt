package dev.guavakt.util.concurrent

interface ListenableFuture<V> {
    fun isDone(): Boolean
    fun isCancelled(): Boolean
    fun cancel(mayInterruptIfRunning: Boolean): Boolean
    fun get(): V
    fun addListener(listener: () -> Unit)
}
