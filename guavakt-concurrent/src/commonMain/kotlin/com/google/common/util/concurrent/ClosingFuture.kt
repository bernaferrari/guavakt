package dev.guavakt.util.concurrent

/**
 * Guava ClosingFuture — future that runs registered closeables when complete.
 */
class ClosingFuture<V> private constructor(
    private val future: ListenableFuture<V>,
) : ListenableFuture<V> {
    private val closeables = ArrayList<AutoCloseable>()
    private val closed = booleanArrayOf(false)

    init {
        future.addListener { closeAll() }
    }

    fun <C : AutoCloseable> eventuallyWillClose(closeable: C): C {
        monitorSync(this) {
            if (closed[0]) {
                try { closeable.close() } catch (_: Throwable) {}
            } else {
                closeables.add(closeable)
            }
        }
        return closeable
    }

    private fun closeAll() {
        var snapshot: List<AutoCloseable> = emptyList()
        var run = false
        monitorSync(this) {
            if (closed[0]) return
            closed[0] = true
            snapshot = closeables.toList()
            closeables.clear()
            run = true
        }
        if (!run) return
        for (c in snapshot) {
            try { c.close() } catch (_: Throwable) {}
        }
    }

    override fun isDone(): Boolean = future.isDone()
    override fun isCancelled(): Boolean = future.isCancelled()
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = future.cancel(mayInterruptIfRunning)
    override fun get(): V = future.get()
    override fun addListener(listener: () -> Unit) = future.addListener(listener)

    fun finishToFuture(): ListenableFuture<V> = future

    companion object {
        fun <V> from(future: ListenableFuture<V>): ClosingFuture<V> = ClosingFuture(future)
        fun <V> submit(callable: () -> V, executor: ListeningExecutorService): ClosingFuture<V> =
            from(executor.submit(callable))
    }
}
