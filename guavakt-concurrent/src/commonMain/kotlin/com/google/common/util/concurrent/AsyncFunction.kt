package dev.guavakt.util.concurrent

/**
 * Guava-shaped asynchronous transformation.
 *
 * New coroutine code should generally use a suspending lambda. This interface exists for
 * migration and composes through [Futures.transformAsync]. Returning `null` is not supported.
 */
fun interface AsyncFunction<I, O> {
    fun apply(input: I): ListenableFuture<O>
}
