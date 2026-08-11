package dev.guavakt.util.concurrent

open class WrappingExecutorService(
    private val delegate: ListeningExecutorService = MoreExecutors.directExecutor(),
) : AbstractListeningExecutorService() {
    protected open fun wrapTask(command: () -> Unit): () -> Unit = command
    override fun execute(command: () -> Unit) = delegate.execute(wrapTask(command))
}
