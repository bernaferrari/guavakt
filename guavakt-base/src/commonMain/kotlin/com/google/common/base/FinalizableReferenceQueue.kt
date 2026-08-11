package dev.guavakt.base

import dev.guavakt.base.internal.PlatformFinalizerQueue
import dev.guavakt.base.internal.PlatformFinalizerReference

/**
 * Queue for [FinalizableReference] callbacks.
 *
 * JVM dispatches callbacks from a daemon reference-queue worker and [cleanUp] drains work that
 * has already been enqueued. JS, Wasm, and Native use explicit strong stand-ins: callbacks are
 * never GC-driven there, so call [FinalizableReference.finalizeReferent] yourself when needed.
 */
class FinalizableReferenceQueue {
    private val platformQueue = PlatformFinalizerQueue()
    private var closed = false

    fun cleanUp() {
        if (!closed) platformQueue.cleanUp()
    }

    fun close() {
        cleanUp()
        closed = true
        platformQueue.close()
    }

    fun isClosed(): Boolean = closed

    /** Number of callbacks dispatched automatically or through [cleanUp]. */
    fun cleanupCount(): Int = platformQueue.cleanupCount()

    internal fun <T : Any> weakReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> {
        check(!closed) { "FinalizableReferenceQueue is closed" }
        return platformQueue.weakReference(referent, onCollected)
    }

    internal fun <T : Any> softReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> {
        check(!closed) { "FinalizableReferenceQueue is closed" }
        return platformQueue.softReference(referent, onCollected)
    }

    internal fun <T : Any> phantomReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> {
        check(!closed) { "FinalizableReferenceQueue is closed" }
        return platformQueue.phantomReference(referent, onCollected)
    }
}
