package dev.guavakt.base

import dev.guavakt.base.internal.PlatformFinalizerReference

/**
 * Phantom-shaped reference: [get] always returns null and the referent is not held strongly on JVM.
 * Automatic `finalizeReferent` dispatch is not available on non-JVM targets.
 */
open class FinalizablePhantomReference<T : Any>(
    referent: T,
    queue: FinalizableReferenceQueue,
) : FinalizableReference {
    private val reference: PlatformFinalizerReference<T> =
        queue.phantomReference(referent) { finalizeReferent() }
    open fun get(): T? = null
    open fun clear() { reference.clear() }
    open override fun finalizeReferent() { clear() }
}
