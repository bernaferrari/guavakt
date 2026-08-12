package com.bernaferrari.guavakt.base

/**
 * Guava-style finalization hook. JVM [FinalizableReferenceQueue] dispatches it after collection;
 * other targets require an explicit call because portable GC-reference queues do not exist.
 */
interface FinalizableReference {
    fun finalizeReferent()
}
