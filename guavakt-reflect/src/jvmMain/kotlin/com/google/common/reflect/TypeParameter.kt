package dev.guavakt.reflect

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

/**
 * JVM-only capture of a free Java type variable for Guava-shaped reflective code.
 *
 * Kotlin common code cannot preserve Java [TypeVariable] identity; use reified Kotlin APIs there.
 */
abstract class TypeParameter<T> protected constructor() {
    internal val typeVariable: TypeVariable<*>

    init {
        val superclass = javaClass.genericSuperclass
        val captured = (superclass as? ParameterizedType)?.actualTypeArguments?.singleOrNull()
        require(captured is TypeVariable<*>) { "$captured should be a type variable." }
        typeVariable = captured
    }

    final override fun equals(other: Any?): Boolean =
        other is TypeParameter<*> && typeVariable == other.typeVariable

    final override fun hashCode(): Int = typeVariable.hashCode()

    override fun toString(): String = typeVariable.toString()
}
