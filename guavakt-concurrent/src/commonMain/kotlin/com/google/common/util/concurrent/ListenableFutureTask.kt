package dev.guavakt.util.concurrent

/**
 * Guava ListenableFutureTask — runnable future that is also a [ListenableFuture].
 */
open class ListenableFutureTask<V> protected constructor(
    private val callable: () -> V,
) : ListenableFuture<V> {
    private val result = SettableFuture.create<V>()
    override fun isDone(): Boolean = result.isDone()
    override fun isCancelled(): Boolean = result.isCancelled()
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = result.cancel(mayInterruptIfRunning)
    override fun get(): V = result.get()
    override fun addListener(listener: () -> Unit) = result.addListener(listener)
    fun run() = invoke()
    operator fun invoke() {
        if (result.isDone()) return
        try { result.set(callable()) } catch (t: Throwable) { result.setException(t) }
    }
    protected fun set(value: V): Boolean = result.set(value)
    protected fun setException(t: Throwable): Boolean = result.setException(t)
    companion object {
        fun <V> create(callable: () -> V): ListenableFutureTask<V> = ListenableFutureTask(callable)
        fun <V> create(runnable: () -> Unit, resultValue: V): ListenableFutureTask<V> =
            ListenableFutureTask { runnable(); resultValue }
    }
}
