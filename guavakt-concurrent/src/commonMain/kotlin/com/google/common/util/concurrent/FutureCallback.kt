package dev.guavakt.util.concurrent

/** Receives exactly one successful value or terminal failure from a [ListenableFuture]. */
interface FutureCallback<in V> {
    fun onSuccess(result: V)
    fun onFailure(throwable: Throwable)
}
