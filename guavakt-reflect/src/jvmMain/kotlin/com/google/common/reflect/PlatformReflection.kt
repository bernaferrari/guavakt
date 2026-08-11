package dev.guavakt.reflect

import java.lang.reflect.Proxy

internal actual fun <T : Any> platformNewProxy(
    interfaceType: kotlin.reflect.KClass<T>,
    handler: (methodName: String, args: Array<out Any?>) -> Any?,
): T {
    val jClass = interfaceType.java
    require(jClass.isInterface) { "${interfaceType.simpleName} is not an interface" }
    val proxy = Proxy.newProxyInstance(
        jClass.classLoader,
        arrayOf(jClass),
    ) { _, method, args ->
        handler(method.name, args ?: emptyArray())
    }
    @Suppress("UNCHECKED_CAST")
    return proxy as T
}

internal actual fun platformInitialize(vararg classes: kotlin.reflect.KClass<*>) {
    for (c in classes) {
        val javaClass = c.java
        Class.forName(javaClass.name, true, javaClass.classLoader)
    }
}

internal actual fun platformClassDisplayName(clazz: kotlin.reflect.KClass<*>): String =
    clazz.qualifiedName ?: clazz.simpleName ?: "UnknownType"
