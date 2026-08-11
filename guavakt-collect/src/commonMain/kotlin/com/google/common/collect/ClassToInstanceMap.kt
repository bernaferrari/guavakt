package dev.guavakt.collect

import kotlin.reflect.KClass

/** A type-safe heterogeneous map keyed by the runtime class of each value. */
interface ClassToInstanceMap<B : Any> : Map<KClass<out B>, B> {
    fun <T : B> getInstance(type: KClass<T>): T?

    /** Associates [type] with [value], returning the previous typed value. */
    fun <T : B> putInstance(type: KClass<T>, value: T): T?
}
