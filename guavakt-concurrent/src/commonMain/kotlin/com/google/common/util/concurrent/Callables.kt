package dev.guavakt.util.concurrent

/** Guava Callables utilities (KMP; no thread interruption helpers that need JVM Thread). */
object Callables {
    fun <T> returning(value: T): () -> T = { value }

    fun <T> withInitialValue(value: T): () -> T = returning(value)

    fun <T> asyncReturning(value: T): () -> ListenableFuture<T> = {
        Futures.immediateFuture(value)
    }
}
