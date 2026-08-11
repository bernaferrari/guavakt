package dev.guavakt.primitives

/**
 * Guava Primitives — maps between primitive KClass and wrapper KClass (KMP uses Kotlin types).
 */
object Primitives {
    private val PRIMITIVE_TO_WRAPPER: Map<kotlin.reflect.KClass<*>, kotlin.reflect.KClass<*>> = mapOf(
        Boolean::class to Boolean::class,
        Byte::class to Byte::class,
        Char::class to Char::class,
        Double::class to Double::class,
        Float::class to Float::class,
        Int::class to Int::class,
        Long::class to Long::class,
        Short::class to Short::class,
    )

    private val WRAPPER_TO_PRIMITIVE: Map<kotlin.reflect.KClass<*>, kotlin.reflect.KClass<*>> =
        PRIMITIVE_TO_WRAPPER.entries.associate { (k, v) -> v to k }

    private val ALL_PRIMITIVE_TYPES: Set<kotlin.reflect.KClass<*>> = PRIMITIVE_TO_WRAPPER.keys
    private val ALL_WRAPPER_TYPES: Set<kotlin.reflect.KClass<*>> = PRIMITIVE_TO_WRAPPER.values.toSet()

    fun allPrimitiveTypes(): Set<kotlin.reflect.KClass<*>> = ALL_PRIMITIVE_TYPES
    fun allWrapperTypes(): Set<kotlin.reflect.KClass<*>> = ALL_WRAPPER_TYPES

    fun isWrapperType(type: kotlin.reflect.KClass<*>): Boolean = type in ALL_WRAPPER_TYPES

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> wrap(type: kotlin.reflect.KClass<T>): kotlin.reflect.KClass<T> =
        (PRIMITIVE_TO_WRAPPER[type] ?: type) as kotlin.reflect.KClass<T>

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> unwrap(type: kotlin.reflect.KClass<T>): kotlin.reflect.KClass<T> =
        (WRAPPER_TO_PRIMITIVE[type] ?: type) as kotlin.reflect.KClass<T>
}
