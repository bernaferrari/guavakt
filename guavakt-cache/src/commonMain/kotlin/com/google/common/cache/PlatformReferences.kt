package dev.guavakt.cache

import dev.guavakt.base.internal.PlatformSoftRef as BaseSoft
import dev.guavakt.base.internal.PlatformWeakRef as BaseWeak
import dev.guavakt.base.internal.Strength as BaseStrength
import dev.guavakt.base.internal.platformIdentityHashCode as baseIdentityHashCode
import dev.guavakt.base.internal.platformSupportsWeakReferences as baseSupportsWeak
import dev.guavakt.base.internal.pollClearedWeakOrSoftReferences as basePoll

/** @see dev.guavakt.base.internal.Strength */
typealias Strength = BaseStrength

fun platformSupportsWeakReferences(): Boolean = baseSupportsWeak()

fun pollClearedWeakOrSoftReferences(): Int = basePoll()

fun platformIdentityHashCode(value: Any): Int = baseIdentityHashCode(value)

/** Thin wrappers so call sites keep [PlatformWeakRef] in cache package. */
class PlatformWeakRef<T : Any>(referent: T) {
    private val impl = BaseWeak(referent)
    fun get(): T? = impl.get()
    fun clear() = impl.clear()
}

class PlatformSoftRef<T : Any>(referent: T) {
    private val impl = BaseSoft(referent)
    fun get(): T? = impl.get()
    fun clear() = impl.clear()
}
