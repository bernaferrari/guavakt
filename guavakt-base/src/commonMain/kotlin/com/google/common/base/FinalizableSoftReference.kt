package dev.guavakt.base

import dev.guavakt.base.internal.PlatformFinalizerReference

/** JVM soft reference; unsupported targets use the documented strong stand-in. */
open class FinalizableSoftReference<T : Any>(
    referent: T,
    queue: FinalizableReferenceQueue,
) : FinalizableReference {
    private val reference: PlatformFinalizerReference<T> =
        queue.softReference(referent) { finalizeReferent() }
    open fun get(): T? = reference.get()
    open fun clear() { reference.clear() }
    open override fun finalizeReferent() { clear() }
}
