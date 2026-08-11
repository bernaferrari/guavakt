package dev.guavakt.util.concurrent

class TrustedListenableFutureTask<V> private constructor(callable: () -> V) :
    ListenableFutureTask<V>(callable) {
    companion object {
        fun <V> create(callable: () -> V): TrustedListenableFutureTask<V> =
            TrustedListenableFutureTask(callable)
    }
}
