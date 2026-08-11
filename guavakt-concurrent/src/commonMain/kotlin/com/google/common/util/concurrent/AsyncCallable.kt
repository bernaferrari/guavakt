package dev.guavakt.util.concurrent

/**
 * Guava-shaped callable that starts work and returns its future.
 *
 * New coroutine code should use [CoroutineScope.future][future]. This interface is retained for
 * migration and can be scheduled through [Futures.submitAsync]. Returning `null` is not supported.
 */
fun interface AsyncCallable<V> {
    fun call(): ListenableFuture<V>
}
