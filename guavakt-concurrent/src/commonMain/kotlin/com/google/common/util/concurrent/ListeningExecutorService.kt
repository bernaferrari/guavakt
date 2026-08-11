package dev.guavakt.util.concurrent

interface ListeningExecutorService {
    fun execute(command: () -> Unit)
    fun <T> submit(task: () -> T): ListenableFuture<T>
    fun shutdown() {}
    fun isShutdown(): Boolean = false
}
