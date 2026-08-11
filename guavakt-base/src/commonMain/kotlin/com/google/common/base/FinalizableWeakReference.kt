package dev.guavakt.base

import dev.guavakt.base.internal.PlatformFinalizerReference

/**
 * Weak reference on JVM. Targets without GC-reference support use the documented
 * strong stand-in; automatic finalization is unavailable there.
 */
open class FinalizableWeakReference<T : Any>(
    referent: T,
    queue: FinalizableReferenceQueue,
) : FinalizableReference {
    private val reference: PlatformFinalizerReference<T> =
        queue.weakReference(referent) { finalizeReferent() }
    open fun get(): T? = reference.get()
    open fun clear() { reference.clear() }
    open override fun finalizeReferent() { clear() }
}
