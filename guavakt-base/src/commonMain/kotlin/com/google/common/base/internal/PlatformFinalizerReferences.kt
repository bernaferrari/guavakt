package dev.guavakt.base.internal

/**
 * Platform queue for finalizable references. JVM enqueues weak, soft, and phantom references and
 * dispatches their callbacks; targets without GC-reference queues retain documented strong
 * stand-ins and never invoke callbacks automatically.
 */
internal expect class PlatformFinalizerQueue() {
    fun <T : Any> weakReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T>

    fun <T : Any> softReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T>

    fun <T : Any> phantomReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T>

    fun cleanUp(): Int
    fun cleanupCount(): Int
    fun close()
}

internal expect interface PlatformFinalizerReference<T : Any> {
    fun get(): T?
    fun clear()
}
