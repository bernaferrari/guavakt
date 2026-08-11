package dev.guavakt.util.concurrent

abstract class AbstractListeningExecutorService : ListeningExecutorService {
    @kotlin.concurrent.Volatile private var shutdownFlag = false
    abstract override fun execute(command: () -> Unit)
    override fun <T> submit(task: () -> T): ListenableFuture<T> {
        val future = ListenableFutureTask.create(task)
        execute { future.run() }
        return future
    }
    override fun shutdown() { shutdownFlag = true }
    override fun isShutdown(): Boolean = shutdownFlag
}
