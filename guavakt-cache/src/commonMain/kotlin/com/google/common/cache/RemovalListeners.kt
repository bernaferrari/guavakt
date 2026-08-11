package dev.guavakt.cache

/** Guava RemovalListeners — async listener wrapper. */
object RemovalListeners {
    fun interface ExecutorLike {
        fun execute(command: () -> Unit)
    }

    fun <K, V> asynchronous(
        listener: (RemovalNotification<K, V>) -> Unit,
        executor: ExecutorLike,
    ): (RemovalNotification<K, V>) -> Unit = { notification ->
        executor.execute { listener(notification) }
    }

    /** Guava-shaped typed listener overload; notifications are handed to [executor] in order. */
    fun <K, V> asynchronous(
        listener: RemovalListener<K, V>,
        executor: ExecutorLike,
    ): RemovalListener<K, V> = RemovalListener { notification ->
        executor.execute { listener.onRemoval(notification) }
    }
}
