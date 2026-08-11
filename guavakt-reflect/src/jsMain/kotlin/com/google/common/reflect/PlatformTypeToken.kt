package dev.guavakt.reflect

import kotlin.reflect.KClass

internal actual fun kClassIsSubtypeOf(sub: KClass<*>, sup: KClass<*>): Boolean =
    sub == sup

internal actual fun platformTypeHierarchy(type: KClass<*>): List<KClass<*>> = emptyList()
