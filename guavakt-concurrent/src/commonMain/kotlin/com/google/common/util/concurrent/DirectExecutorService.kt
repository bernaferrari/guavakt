package dev.guavakt.util.concurrent

class DirectExecutorService : AbstractListeningExecutorService() {
    override fun execute(command: () -> Unit) = command()
}
