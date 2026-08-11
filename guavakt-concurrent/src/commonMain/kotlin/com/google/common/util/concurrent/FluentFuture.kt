package dev.guavakt.util.concurrent

/**
 * Guava FluentFuture — fluent wrappers around [ListenableFuture] transforms.
 */
open class FluentFuture<V> protected constructor(
    private val delegate: ListenableFuture<V>,
) : ListenableFuture<V> {
    override fun isDone(): Boolean = delegate.isDone()
    override fun isCancelled(): Boolean = delegate.isCancelled()
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean =
        delegate.cancel(mayInterruptIfRunning)
    override fun get(): V = delegate.get()
    override fun addListener(listener: () -> Unit) = delegate.addListener(listener)

    fun <T> transform(function: (V) -> T): FluentFuture<T> =
        from(AbstractTransformFuture.create(delegate, function))

    fun <X : Throwable> catching(
        exceptionType: kotlin.reflect.KClass<X>,
        fallback: (X) -> V,
    ): FluentFuture<V> = from(AbstractCatchingFuture.create(delegate, exceptionType, fallback))

    companion object {
        fun <V> from(future: ListenableFuture<V>): FluentFuture<V> =
            if (future is FluentFuture) future else FluentFuture(future)
    }
}
