package dev.guavakt.base.internal

/** Reachability strength for maps/caches (Guava MapMaker / CacheBuilder). */
enum class Strength {
    STRONG,
    WEAK,
    SOFT,
}

expect fun platformSupportsWeakReferences(): Boolean

expect class PlatformWeakRef<T : Any>(referent: T) {
    fun get(): T?
    fun clear()
}

expect class PlatformSoftRef<T : Any>(referent: T) {
    fun get(): T?
    fun clear()
}

expect fun pollClearedWeakOrSoftReferences(): Int

expect fun platformIdentityHashCode(value: Any): Int
