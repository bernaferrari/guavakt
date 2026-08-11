package dev.guavakt.parity

import com.google.common.reflect.AbstractInvocationHandler as GuavaAbstractInvocationHandler
import com.google.common.reflect.TypeParameter as GuavaTypeParameter
import dev.guavakt.reflect.AbstractInvocationHandler as GuavaKtAbstractInvocationHandler
import dev.guavakt.reflect.TypeParameter as GuavaKtTypeParameter
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReflectionHandlerDifferentialTest {
    private interface Echo {
        fun echo(value: String): String
        fun noArgs(): Int
    }

    private interface Marker

    @Test
    fun invocationAndObjectMethodDispatchMatchGuava() {
        val guavaHandler = GuavaHandler("same")
        val guavaKtHandler = GuavaKtHandler("same")
        val guava = proxy(Echo::class.java, guavaHandler)
        val guavaKt = proxy(Echo::class.java, guavaKtHandler)

        assertEquals(guava.echo("hello"), guavaKt.echo("hello"))
        assertEquals(guava.noArgs(), guavaKt.noArgs())
        assertEquals(guavaHandler.calls, guavaKtHandler.calls)
        assertEquals(guava.toString(), guavaKt.toString())
        assertEquals(guava.hashCode(), guavaKt.hashCode())
        assertEquals(2, guavaHandler.calls)
        assertEquals(2, guavaKtHandler.calls)
    }

    @Test
    fun proxyEqualityRulesMatchGuava() {
        val guavaA = proxy(Echo::class.java, GuavaHandler("same"))
        val guavaB = proxy(Echo::class.java, GuavaHandler("same"))
        val guavaDifferent = proxy(Echo::class.java, GuavaHandler("different"))
        val guavaKtA = proxy(Echo::class.java, GuavaKtHandler("same"))
        val guavaKtB = proxy(Echo::class.java, GuavaKtHandler("same"))
        val guavaKtDifferent = proxy(Echo::class.java, GuavaKtHandler("different"))

        assertTrue(guavaA == guavaA)
        assertTrue(guavaKtA == guavaKtA)
        assertEquals(guavaA == guavaB, guavaKtA == guavaKtB)
        assertEquals(guavaA == guavaDifferent, guavaKtA == guavaKtDifferent)
        assertFalse(guavaA.equals(null))
        assertFalse(guavaKtA.equals(null))

        val guavaOtherOrder = multiProxy(arrayOf(Marker::class.java, Echo::class.java), GuavaHandler("same"))
        val guavaKtOtherOrder = multiProxy(arrayOf(Marker::class.java, Echo::class.java), GuavaKtHandler("same"))
        assertEquals(guavaA == guavaOtherOrder, guavaKtA == guavaKtOtherOrder)
    }

    @Test
    fun typeVariableCaptureEqualityAndConcreteRejectionMatchGuava() {
        compareCapturedTypeParameters<String, Int>()
        assertFailsWith<IllegalArgumentException> { object : GuavaTypeParameter<String>() {} }
        assertFailsWith<IllegalArgumentException> { object : GuavaKtTypeParameter<String>() {} }
    }

    private fun <A : Any, B : Any> compareCapturedTypeParameters() {
        val guavaA1 = object : GuavaTypeParameter<A>() {}
        val guavaA2 = object : GuavaTypeParameter<A>() {}
        val guavaB = object : GuavaTypeParameter<B>() {}
        val guavaKtA1 = object : GuavaKtTypeParameter<A>() {}
        val guavaKtA2 = object : GuavaKtTypeParameter<A>() {}
        val guavaKtB = object : GuavaKtTypeParameter<B>() {}

        assertEquals(guavaA1 == guavaA2, guavaKtA1 == guavaKtA2)
        assertEquals(guavaA1 == guavaB, guavaKtA1 == guavaKtB)
        assertEquals(guavaA1.hashCode() == guavaA2.hashCode(), guavaKtA1.hashCode() == guavaKtA2.hashCode())
        assertEquals(guavaA1.toString(), guavaKtA1.toString())
    }

    private class GuavaHandler(private val identity: String) : GuavaAbstractInvocationHandler() {
        var calls = 0

        override fun handleInvocation(proxy: Any, method: Method, args: Array<out Any?>): Any? {
            calls++
            return if (method.name == "echo") args[0] else args.size
        }

        override fun equals(other: Any?): Boolean = other is GuavaHandler && identity == other.identity
        override fun hashCode(): Int = identity.hashCode()
        override fun toString(): String = "handler:$identity"
    }

    private class GuavaKtHandler(private val identity: String) : GuavaKtAbstractInvocationHandler() {
        var calls = 0

        override fun handleInvocation(proxy: Any, method: Method, args: Array<out Any?>): Any? {
            calls++
            return if (method.name == "echo") args[0] else args.size
        }

        override fun equals(other: Any?): Boolean = other is GuavaKtHandler && identity == other.identity
        override fun hashCode(): Int = identity.hashCode()
        override fun toString(): String = "handler:$identity"
    }

    private fun <T> proxy(type: Class<T>, handler: java.lang.reflect.InvocationHandler): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler) as T
    }

    private fun multiProxy(
        interfaces: Array<Class<*>>,
        handler: java.lang.reflect.InvocationHandler,
    ): Any = Proxy.newProxyInstance(javaClass.classLoader, interfaces, handler)
}
