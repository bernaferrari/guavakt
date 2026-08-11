package dev.guavakt.util.concurrent

/**
 * Guava JdkFutureAdapters — adapt a completed-value supplier into ListenableFuture (no JDK Future on KMP).
 */
object JdkFutureAdapters {
    fun <V> listenInPoolThread(futureGet: () -> V): ListenableFuture<V> {
        val out = SettableFuture.create<V>()
        try {
            out.set(futureGet())
        } catch (t: Throwable) {
            out.setException(t)
        }
        return out
    }
}
