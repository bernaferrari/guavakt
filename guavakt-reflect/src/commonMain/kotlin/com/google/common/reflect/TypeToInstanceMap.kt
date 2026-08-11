package dev.guavakt.reflect

import kotlin.reflect.KClass

/**
 * A heterogeneous map keyed by [TypeToken]. GuavaKt tokens retain raw [KClass] identity in common
 * code; generic arguments are not reified, and JVM `java.lang.reflect.Type` parity is not claimed.
 */
interface TypeToInstanceMap<B : Any> : Map<TypeToken<out B>, B> {
    fun <T : B> getInstance(type: TypeToken<T>): T?
    fun <T : B> getInstance(type: KClass<T>): T?
}
