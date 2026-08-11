package dev.guavakt.reflect

import kotlin.reflect.KClass

internal actual fun kClassIsSubtypeOf(sub: KClass<*>, sup: KClass<*>): Boolean =
    sup.java.isAssignableFrom(sub.java)

internal actual fun platformTypeHierarchy(type: KClass<*>): List<KClass<*>> {
    val out = ArrayList<KClass<*>>()
    val j = type.java
    j.superclass?.kotlin?.let { out.add(it) }
    for (i in j.interfaces) out.add(i.kotlin)
    return out
}
