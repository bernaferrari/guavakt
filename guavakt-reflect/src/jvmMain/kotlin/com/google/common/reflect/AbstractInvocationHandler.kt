package dev.guavakt.reflect

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * JVM-only [InvocationHandler] base that gives dynamic proxies stable object-method semantics.
 *
 * `hashCode` and `toString` dispatch to the handler. Equality requires proxies with the same
 * ordered interface list and then compares their handlers. All other calls reach
 * [handleInvocation] with a non-null argument array.
 */
abstract class AbstractInvocationHandler : InvocationHandler {
    final override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val actualArgs = args ?: EMPTY_ARGS
        if (actualArgs.isEmpty() && method.name == "hashCode") return hashCode()
        if (
            actualArgs.size == 1 &&
            method.name == "equals" &&
            method.parameterTypes.contentEquals(arrayOf(Any::class.java))
        ) {
            val argument = actualArgs[0] ?: return false
            if (proxy === argument) return true
            return isProxyOfSameInterfaces(argument, proxy.javaClass) &&
                equals(Proxy.getInvocationHandler(argument))
        }
        if (actualArgs.isEmpty() && method.name == "toString") return toString()
        return handleInvocation(proxy, method, actualArgs)
    }

    protected abstract fun handleInvocation(
        proxy: Any,
        method: Method,
        args: Array<out Any?>,
    ): Any?

    private fun isProxyOfSameInterfaces(argument: Any, proxyClass: Class<*>): Boolean =
        proxyClass.isInstance(argument) ||
            (Proxy.isProxyClass(argument.javaClass) &&
                argument.javaClass.interfaces.contentEquals(proxyClass.interfaces))

    private companion object {
        val EMPTY_ARGS: Array<out Any?> = emptyArray()
    }
}
