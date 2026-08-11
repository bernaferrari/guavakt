package dev.guavakt.util.concurrent

/**
 * Guava ExecutionSequencer — serializes async tasks so each starts after the previous completes.
 */
class ExecutionSequencer private constructor() {
    private val lock = Any()
    private var nextTask: (() -> Unit)? = null
    private var running = false

    fun <T> submit(callable: () -> T, executor: DirectExecutorLike): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        val task = {
            try {
                future.set(callable())
            } catch (t: Throwable) {
                future.setException(t)
            }
        }
        monitorSync(lock) {
            if (!running) {
                running = true
                executor.execute {
                    try {
                        task()
                    } finally {
                        scheduleNext(executor)
                    }
                }
            } else {
                val prev = nextTask
                nextTask = {
                    prev?.invoke()
                    executor.execute {
                        try {
                            task()
                        } finally {
                            scheduleNext(executor)
                        }
                    }
                }
            }
        }
        return future
    }

    private fun scheduleNext(executor: DirectExecutorLike) {
        var next: (() -> Unit)? = null
        monitorSync(lock) {
            next = nextTask
            nextTask = null
            if (next == null) running = false
        }
        next?.invoke()
    }

    companion object {
        fun create(): ExecutionSequencer = ExecutionSequencer()
    }
}
