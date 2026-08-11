package dev.guavakt.util.concurrent

/** Guava DirectExecutor — runs command on the calling thread. */
enum class DirectExecutor {
    INSTANCE;
    fun execute(command: () -> Unit) = command()
}
