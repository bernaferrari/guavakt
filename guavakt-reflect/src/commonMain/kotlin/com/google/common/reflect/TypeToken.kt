package dev.guavakt.reflect

import kotlin.reflect.KClass

/**
 * Guava TypeToken — type literal / raw class token for KMP.
 *
 * **commonMain:** [KClass] erasure with [isSubtypeOf] via platform assignability
 * (JVM: `Class.isAssignableFrom`; other targets: equality / simple hierarchy where available).
 *
 * **Not ported (full Guava JVM depth):** `java.lang.reflect.Type` resolution, wildcards,
 * invokables over generic methods. Prefer `kotlin.reflect` when you need that depth.
 */
open class TypeToken<T : Any> protected constructor(
    private val raw: KClass<T>,
) {
    open fun getRawType(): KClass<T> = raw

    open fun getType(): KClass<T> = raw

    open fun isSubtypeOf(type: TypeToken<*>): Boolean = kClassIsSubtypeOf(raw, type.raw)

    open fun isSubtypeOf(type: KClass<*>): Boolean = kClassIsSubtypeOf(raw, type)

    open fun isSupertypeOf(type: TypeToken<*>): Boolean = type.isSubtypeOf(this)

    open fun isSupertypeOf(type: KClass<*>): Boolean = kClassIsSubtypeOf(type, raw)

    open fun isPrimitive(): Boolean = raw in PRIMITIVES

    /**
     * Type hierarchy tokens. Without full `kotlin-reflect` metadata on the classpath this
     * returns at least `{this}`; on JVM, [platformTypeHierarchy] adds superclass/interfaces.
     */
    open fun getTypes(): Set<TypeToken<*>> {
        val out = LinkedHashSet<TypeToken<*>>()
        out.add(this)
        for (k in platformTypeHierarchy(raw)) {
            if (k == Any::class) continue
            @Suppress("UNCHECKED_CAST")
            out.add(of(k as KClass<Any>))
        }
        return out
    }

    open fun <S : Any> getSubtype(subclass: KClass<S>): TypeToken<S> {
        require(kClassIsSubtypeOf(subclass, raw)) { "$subclass is not a subtype of $raw" }
        return of(subclass)
    }

    open fun <S : Any> getSupertype(superclass: KClass<S>): TypeToken<S> {
        require(kClassIsSubtypeOf(raw, superclass)) { "$superclass is not a supertype of $raw" }
        return of(superclass)
    }

    open fun unwrap(): TypeToken<*> = this

    open fun wrap(): TypeToken<*> = this

    override fun equals(other: Any?): Boolean = other is TypeToken<*> && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw.simpleName ?: raw.toString()

    companion object {
        private val PRIMITIVES: Set<KClass<*>> = setOf(
            Boolean::class, Byte::class, Short::class, Char::class,
            Int::class, Long::class, Float::class, Double::class,
        )

        fun <T : Any> of(clazz: KClass<T>): TypeToken<T> = TypeToken(clazz)

        inline fun <reified T : Any> of(): TypeToken<T> = of(T::class)
    }
}
