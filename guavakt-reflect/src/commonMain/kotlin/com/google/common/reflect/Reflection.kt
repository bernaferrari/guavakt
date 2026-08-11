package dev.guavakt.reflect

/**
 * Guava Reflection — package helpers; [newProxy] is platform-specific (JVM Proxy).
 */
object Reflection {
    fun getPackageName(classSimpleNameWithPackage: String): String {
        val lastDot = classSimpleNameWithPackage.lastIndexOf('.')
        return if (lastDot < 0) "" else classSimpleNameWithPackage.substring(0, lastDot)
    }

    fun getPackageName(clazz: kotlin.reflect.KClass<*>): String =
        getPackageName(platformClassDisplayName(clazz))

    fun <T : Any> newProxy(
        interfaceType: kotlin.reflect.KClass<T>,
        handler: (methodName: String, args: Array<out Any?>) -> Any?,
    ): T = platformNewProxy(interfaceType, handler)

    fun initialize(vararg classes: kotlin.reflect.KClass<*>) {
        platformInitialize(*classes)
    }
}

internal expect fun <T : Any> platformNewProxy(
    interfaceType: kotlin.reflect.KClass<T>,
    handler: (methodName: String, args: Array<out Any?>) -> Any?,
): T

internal expect fun platformInitialize(vararg classes: kotlin.reflect.KClass<*>)
internal expect fun platformClassDisplayName(clazz: kotlin.reflect.KClass<*>): String
