package com.bernaferrari.guavakt.base.internal

import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal actual interface PlatformFinalizerReference<T : Any> {
    actual fun get(): T?
    actual fun clear()
}

private class JvmFinalizerReference<T : Any>(
    private val delegate: Reference<T>,
) : PlatformFinalizerReference<T> {
    override fun get(): T? = delegate.get()
    override fun clear() = delegate.clear()
}

/** JVM reference-queue dispatcher. The daemon mirrors Guava's automatic finalizer behavior. */
internal actual class PlatformFinalizerQueue actual constructor() {
    private val queue = ReferenceQueue<Any>()
    private val lock = Any()
    private val callbacks = LinkedHashMap<Reference<*>, () -> Unit>()
    private val running = AtomicBoolean(true)
    private val dispatchedCount = AtomicInteger()
    private val worker = Thread(::drainBlocking, "GuavaKt-FinalizableReferenceQueue").apply {
        isDaemon = true
        start()
    }

    actual fun <T : Any> weakReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> = register(WeakReference(referent, queue), onCollected)

    actual fun <T : Any> softReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> = register(SoftReference(referent, queue), onCollected)

    actual fun <T : Any> phantomReference(
        referent: T,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> = register(PhantomReference(referent, queue), onCollected)

    private fun <T : Any> register(
        reference: Reference<T>,
        onCollected: () -> Unit,
    ): PlatformFinalizerReference<T> {
        synchronized(lock) {
            check(running.get()) { "FinalizableReferenceQueue is closed" }
            callbacks[reference] = onCollected
        }
        return JvmFinalizerReference(reference)
    }

    actual fun cleanUp(): Int {
        var processed = 0
        while (true) {
            val reference = queue.poll() ?: return processed
            process(reference)
            processed++
        }
    }

    actual fun cleanupCount(): Int = dispatchedCount.get()

    actual fun close() {
        if (!running.getAndSet(false)) return
        worker.interrupt()
        synchronized(lock) {
            callbacks.keys.forEach(Reference<*>::clear)
            callbacks.clear()
        }
    }

    private fun drainBlocking() {
        while (running.get()) {
            try {
                process(queue.remove())
            } catch (_: InterruptedException) {
                // Closing interrupts the daemon; otherwise continue waiting for references.
            }
        }
    }

    private fun process(reference: Reference<*>) {
        val callback = synchronized(lock) { callbacks.remove(reference) } ?: return
        dispatchedCount.incrementAndGet()
        try {
            callback()
        } catch (_: Throwable) {
            // A user finalizer must not terminate the daemon or prevent later queue cleanup.
        }
    }
}
