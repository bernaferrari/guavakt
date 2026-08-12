package com.bernaferrari.guavakt.base.internal

import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

actual fun platformSupportsWeakReferences(): Boolean = true

private val clearedQueue = ReferenceQueue<Any>()

actual class PlatformWeakRef<T : Any> actual constructor(referent: T) {
    private val ref = WeakReference(referent, clearedQueue)
    actual fun get(): T? = ref.get()
    actual fun clear() { ref.clear() }
}

actual class PlatformSoftRef<T : Any> actual constructor(referent: T) {
    private val ref = SoftReference(referent, clearedQueue)
    actual fun get(): T? = ref.get()
    actual fun clear() { ref.clear() }
}

actual fun pollClearedWeakOrSoftReferences(): Int {
    var n = 0
    while (clearedQueue.poll() != null) n++
    return n
}

actual fun platformIdentityHashCode(value: Any): Int = System.identityHashCode(value)
