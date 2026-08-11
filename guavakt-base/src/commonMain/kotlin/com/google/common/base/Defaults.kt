package dev.guavakt.base

/**
 * Guava Defaults — default values for types (KMP: common primitives + null for refs).
 */
object Defaults {
    fun defaultValueForBoolean(): Boolean = false
    fun defaultValueForChar(): Char = '\u0000'
    fun defaultValueForByte(): Byte = 0
    fun defaultValueForShort(): Short = 0
    fun defaultValueForInt(): Int = 0
    fun defaultValueForLong(): Long = 0L
    fun defaultValueForFloat(): Float = 0f
    fun defaultValueForDouble(): Double = 0.0

    /** Guava `defaultValue(Class)` stand-in: returns null for unknown/reference types. */
    fun defaultValue(typeName: String): Any? = when (typeName) {
        "boolean", "kotlin.Boolean", "java.lang.Boolean" -> false
        "char", "kotlin.Char", "java.lang.Character" -> '\u0000'
        "byte", "kotlin.Byte", "java.lang.Byte" -> 0.toByte()
        "short", "kotlin.Short", "java.lang.Short" -> 0.toShort()
        "int", "kotlin.Int", "java.lang.Integer" -> 0
        "long", "kotlin.Long", "java.lang.Long" -> 0L
        "float", "kotlin.Float", "java.lang.Float" -> 0f
        "double", "kotlin.Double", "java.lang.Double" -> 0.0
        else -> null
    }
}
