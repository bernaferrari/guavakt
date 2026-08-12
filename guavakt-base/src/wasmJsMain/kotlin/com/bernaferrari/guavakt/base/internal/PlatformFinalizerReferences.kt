package com.bernaferrari.guavakt.base.internal

internal actual interface PlatformFinalizerReference<T : Any> {
    actual fun get(): T?
    actual fun clear()
}

private class StrongFinalizerReference<T : Any>(
    private var referent: T?,
) : PlatformFinalizerReference<T> {
    override fun get(): T? = referent
    override fun clear() {
        referent = null
    }
}

internal actual class PlatformFinalizerQueue actual constructor() {
    actual fun <T : Any> weakReference(referent: T, onCollected: () -> Unit): PlatformFinalizerReference<T> =
        StrongFinalizerReference(referent)

    actual fun <T : Any> softReference(referent: T, onCollected: () -> Unit): PlatformFinalizerReference<T> =
        StrongFinalizerReference(referent)

    actual fun <T : Any> phantomReference(referent: T, onCollected: () -> Unit): PlatformFinalizerReference<T> =
        StrongFinalizerReference(referent)

    actual fun cleanUp(): Int = 0
    actual fun cleanupCount(): Int = 0
    actual fun close() = Unit
}
