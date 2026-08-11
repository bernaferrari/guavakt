package dev.guavakt.util.concurrent

/**
 * Guava FuturesGetChecked — get() that wraps failures in a declared exception type (simplified KMP).
 */
object FuturesGetChecked {
    fun <V, X : Exception> getChecked(future: ListenableFuture<V>, exceptionClass: kotlin.reflect.KClass<X>): V {
        try {
            return future.get()
        } catch (t: Throwable) {
            val cause = if (t is ExecutionException) (t.cause ?: t) else t
            throw IllegalStateException("getChecked failed for $exceptionClass", cause)
        }
    }
}
