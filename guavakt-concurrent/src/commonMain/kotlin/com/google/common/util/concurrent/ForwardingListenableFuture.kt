package dev.guavakt.util.concurrent

/**
 * Guava ForwardingListenableFuture — forwards all calls to a delegate [ListenableFuture].
 */
abstract class ForwardingListenableFuture<V> : ListenableFuture<V> {
    protected abstract fun delegate(): ListenableFuture<V>
    override fun isDone(): Boolean = delegate().isDone()
    override fun isCancelled(): Boolean = delegate().isCancelled()
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean =
        delegate().cancel(mayInterruptIfRunning)
    override fun get(): V = delegate().get()
    override fun addListener(listener: () -> Unit) = delegate().addListener(listener)
}
