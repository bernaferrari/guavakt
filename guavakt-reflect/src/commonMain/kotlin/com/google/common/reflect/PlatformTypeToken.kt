package dev.guavakt.reflect

import kotlin.reflect.KClass

internal expect fun kClassIsSubtypeOf(sub: KClass<*>, sup: KClass<*>): Boolean

/** Superclass + interfaces of [type] as raw [KClass]es (empty when unsupported). */
internal expect fun platformTypeHierarchy(type: KClass<*>): List<KClass<*>>
