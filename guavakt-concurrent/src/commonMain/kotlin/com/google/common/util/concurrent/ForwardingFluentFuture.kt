package dev.guavakt.util.concurrent

abstract class ForwardingFluentFuture<V> : ListenableFuture<V> {
    protected abstract fun delegate(): FluentFuture<V>
    override fun isDone(): Boolean = delegate().isDone()
    override fun isCancelled(): Boolean = delegate().isCancelled()
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = delegate().cancel(mayInterruptIfRunning)
    override fun get(): V = delegate().get()
    override fun addListener(listener: () -> Unit) = delegate().addListener(listener)
}
