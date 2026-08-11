package dev.guavakt.util.concurrent

class SequentialExecutor(private val delegate: ListeningExecutorService) : ListeningExecutorService {
    private val queue = ArrayDeque<() -> Unit>()
    private var workerRunning = false
    private val lock = Any()

    override fun execute(command: () -> Unit) {
        var startWorker = false
        monitorSync(lock) {
            queue.addLast(command)
            if (!workerRunning) {
                workerRunning = true
                startWorker = true
            }
        }
        if (startWorker) delegate.execute { drain() }
    }

    private fun drain() {
        while (true) {
            var task: (() -> Unit)? = null
            monitorSync(lock) {
                task = queue.removeFirstOrNull()
                if (task == null) workerRunning = false
            }
            val t = task ?: return
            try { t() } catch (_: Throwable) {}
        }
    }

    override fun <T> submit(task: () -> T): ListenableFuture<T> {
        val future = ListenableFutureTask.create(task)
        execute { future.run() }
        return future
    }
}
