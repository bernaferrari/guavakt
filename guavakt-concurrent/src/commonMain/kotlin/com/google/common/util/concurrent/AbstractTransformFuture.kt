package dev.guavakt.util.concurrent

/**
 * Guava AbstractTransformFuture — applies [function] to successful result of [input].
 */
open class AbstractTransformFuture<I, O> private constructor(
    input: ListenableFuture<out I>,
    function: (I) -> O,
) : AbstractFuture<O>() {
    init {
        input.addListener {
            if (input.isCancelled()) {
                cancel(false)
                return@addListener
            }
            try {
                val v = input.get()
                setValue(function(v))
            } catch (t: Throwable) {
                val cause = if (t is ExecutionException) (t.cause ?: t) else t
                setFailure(cause)
            }
        }
    }

    companion object {
        fun <I, O> create(input: ListenableFuture<out I>, function: (I) -> O): ListenableFuture<O> =
            AbstractTransformFuture(input, function)
    }
}
