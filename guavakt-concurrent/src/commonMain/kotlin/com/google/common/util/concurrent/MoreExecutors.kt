package dev.guavakt.util.concurrent

object MoreExecutors {
    fun directExecutor(): ListeningExecutorService = DirectExecutorService()
    fun newDirectExecutorService(): ListeningExecutorService = directExecutor()

    /** Runs each task under a monitor lock so tasks never overlap (sequential on caller/delegate). */
    fun newSequentialExecutor(delegate: ListeningExecutorService): ListeningExecutorService =
        object : AbstractListeningExecutorService() {
            private val lock = Any()
            override fun execute(command: () -> Unit) {
                platformMonitorSync(lock) {
                    delegate.execute(command)
                }
            }
        }

    fun listeningDecorator(delegate: ListeningExecutorService): ListeningExecutorService = delegate
}
