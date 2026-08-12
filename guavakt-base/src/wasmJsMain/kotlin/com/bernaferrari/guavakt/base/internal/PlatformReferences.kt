package com.bernaferrari.guavakt.base.internal

actual fun platformSupportsWeakReferences(): Boolean = false

actual class PlatformWeakRef<T : Any> actual constructor(referent: T) {
    private var value: T? = referent
    actual fun get(): T? = value
    actual fun clear() { value = null }
}

actual class PlatformSoftRef<T : Any> actual constructor(referent: T) {
    private var value: T? = referent
    actual fun get(): T? = value
    actual fun clear() { value = null }
}

actual fun pollClearedWeakOrSoftReferences(): Int = 0

actual fun platformIdentityHashCode(value: Any): Int = value.hashCode()
