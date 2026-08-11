package dev.guavakt.util.concurrent

/** Guava ImmediateFuture — already-completed future. */
internal class ImmediateFuture<V>(private val value: V) : ListenableFuture<V> {
    override fun isDone(): Boolean = true
    override fun isCancelled(): Boolean = false
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
    override fun get(): V = value
    override fun addListener(listener: () -> Unit) { listener() }
}
