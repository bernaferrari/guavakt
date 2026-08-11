package dev.guavakt.util.concurrent

abstract class ForwardingListeningExecutorService : ListeningExecutorService {
    protected abstract fun delegate(): ListeningExecutorService
    override fun execute(command: () -> Unit) = delegate().execute(command)
    override fun <T> submit(task: () -> T): ListenableFuture<T> = delegate().submit(task)
    override fun shutdown() = delegate().shutdown()
    override fun isShutdown(): Boolean = delegate().isShutdown()
}
