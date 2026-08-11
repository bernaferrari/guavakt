package dev.guavakt.util.concurrent

/**
 * Guava AbstractCatchingFuture — on failure of [input] matching [exceptionType], runs fallback.
 */
open class AbstractCatchingFuture<V, X : Throwable> private constructor(
    input: ListenableFuture<out V>,
    exceptionType: kotlin.reflect.KClass<X>,
    fallback: (X) -> V,
) : AbstractFuture<V>() {
    init {
        input.addListener {
            if (input.isCancelled()) {
                cancel(false)
                return@addListener
            }
            try {
                setValue(input.get())
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                if (exceptionType.isInstance(cause)) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        setValue(fallback(cause as X))
                    } catch (e: Throwable) {
                        setFailure(e)
                    }
                } else {
                    setFailure(cause)
                }
            }
        }
    }

    companion object {
        fun <V, X : Throwable> create(
            input: ListenableFuture<out V>,
            exceptionType: kotlin.reflect.KClass<X>,
            fallback: (X) -> V,
        ): ListenableFuture<V> = AbstractCatchingFuture(input, exceptionType, fallback)
    }
}
