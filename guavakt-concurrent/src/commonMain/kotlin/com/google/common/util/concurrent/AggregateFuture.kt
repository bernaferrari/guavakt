package dev.guavakt.util.concurrent

/**
 * Guava AggregateFuture — completes when all [futures] complete; result is list of values (all-as-list).
 */
open class AggregateFuture<V> private constructor(
    futures: Collection<ListenableFuture<out V>>,
    private val allMustSucceed: Boolean,
) : AbstractFuture<List<V>>() {
    private val remaining = futures.size
    private val values = arrayOfNulls<Any?>(futures.size)
    private var remainingCount = futures.size
    private val lock = Any()

    init {
        if (futures.isEmpty()) {
            @Suppress("UNCHECKED_CAST")
            setValue(emptyList())
        } else {
            futures.forEachIndexed { index, future ->
                future.addListener {
                    try {
                        if (future.isCancelled()) {
                            if (allMustSucceed) cancel(false)
                            else record(index, null)
                        } else {
                            record(index, future.get())
                        }
                    } catch (t: Throwable) {
                        if (allMustSucceed) {
                            val cause = if (t is ExecutionException) (t.cause ?: t) else t
                            setFailure(cause)
                        } else {
                            record(index, null)
                        }
                    }
                }
            }
        }
    }

    private fun record(index: Int, value: V?) {
        var doneNow = false
        monitorSync(lock) {
            if (isDone()) return
            values[index] = value
            remainingCount--
            if (remainingCount == 0) doneNow = true
        }
        if (doneNow) {
            @Suppress("UNCHECKED_CAST")
            setValue(values.map { it as V })
        }
    }

    companion object {
        fun <V> create(futures: Collection<ListenableFuture<out V>>, allMustSucceed: Boolean): ListenableFuture<List<V>> =
            AggregateFuture(futures, allMustSucceed)
    }
}
