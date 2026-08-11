package dev.guavakt.reflect

internal actual fun <T : Any> platformNewProxy(
    interfaceType: kotlin.reflect.KClass<T>,
    handler: (methodName: String, args: Array<out Any?>) -> Any?,
): T = throw UnsupportedOperationException(
    "Reflection.newProxy requires JVM (java.lang.reflect.Proxy); not available on Wasm",
)

internal actual fun platformInitialize(vararg classes: kotlin.reflect.KClass<*>) {
    // no Class.forName side effects
}

internal actual fun platformClassDisplayName(clazz: kotlin.reflect.KClass<*>): String =
    clazz.toString().removePrefix("class ")
