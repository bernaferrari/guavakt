package dev.guavakt.util.concurrent

/**
 * Guava ExecutionList — runs listeners once when executed (thread-safe).
 */
class ExecutionList {
    private val lock = Any()
    private var executed = false
    private val listeners = ArrayList<Pair<() -> Unit, DirectExecutorLike>>()

    fun add(runnable: () -> Unit, executor: DirectExecutorLike) {
        monitorSync(lock) {
            if (!executed) {
                listeners.add(runnable to executor)
                return
            }
        }
        executeListener(runnable, executor)
    }

    fun execute() {
        var snapshot: List<Pair<() -> Unit, DirectExecutorLike>> = emptyList()
        var run = false
        monitorSync(lock) {
            if (executed) return
            executed = true
            snapshot = listeners.toList()
            listeners.clear()
            run = true
        }
        if (!run) return
        for ((r, e) in snapshot) executeListener(r, e)
    }

    private fun executeListener(runnable: () -> Unit, executor: DirectExecutorLike) {
        try {
            executor.execute(runnable)
        } catch (_: Throwable) {
            // Guava logs; KMP swallows to keep listeners isolated
        }
    }
}

fun interface DirectExecutorLike {
    fun execute(command: () -> Unit)
}
