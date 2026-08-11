package dev.guavakt.parity

import com.google.common.reflect.Invokable as GuavaInvokable
import dev.guavakt.reflect.Invokable as GuavaKtInvokable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvokableDifferentialTest {
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
    private annotation class Marked

    private class Fixture @Marked constructor(private val prefix: String) {
        @Marked
        fun combine(@Marked value: String): CharSequence = prefix + value

        private fun secret(): String = "secret:$prefix"

        @Throws(IllegalArgumentException::class)
        fun risky(): String = prefix

        fun join(vararg values: String): String = values.joinToString(prefix = prefix)

        fun <T> identity(value: T): T = value
    }

    @Test
    fun methodMetadataInvocationAndReturnSpecializationMatchGuava() {
        val method = Fixture::class.java.getDeclaredMethod("combine", String::class.java)
        @Suppress("UNCHECKED_CAST")
        val guava = GuavaInvokable.from(method) as GuavaInvokable<Fixture, Any>
        val guavaKt = GuavaKtInvokable.from(method)

        assertEquals(guava.name, guavaKt.getName())
        assertEquals(guava.modifiers, guavaKt.getModifiers())
        assertEquals(guava.isPublic, guavaKt.isPublic())
        assertEquals(guava.isPrivate, guavaKt.isPrivate())
        assertEquals(guava.isProtected, guavaKt.isProtected())
        assertEquals(guava.isPackagePrivate, guavaKt.isPackagePrivate())
        assertEquals(guava.isStatic, guavaKt.isStatic())
        assertEquals(guava.isFinal, guavaKt.isFinal())
        assertEquals(guava.isAbstract, guavaKt.isAbstract())
        assertEquals(guava.isNative, guavaKt.isNative())
        assertEquals(guava.isSynchronized, guavaKt.isSynchronized())
        assertEquals(guava.isSynthetic, guavaKt.isSynthetic())
        assertEquals(guava.isOverridable, guavaKt.isOverridable())
        assertEquals(guava.isVarArgs, guavaKt.isVarArgs())
        assertEquals(
            guava.isAnnotationPresent(Marked::class.java),
            guavaKt.isAnnotationPresent(Marked::class.java),
        )
        assertEquals(guava.returnType.rawType.name, guavaKt.getReturnType().getRawType().java.name)
        assertEquals(guava.ownerType.rawType.name, guavaKt.getOwnerType().getRawType().java.name)
        assertEquals(guava.declaringClass.name, guavaKt.getDeclaringClass().java.name)
        assertEquals(guava.toString(), guavaKt.toString())
        assertEquals(guava.hashCode(), guavaKt.hashCode())

        guava.setAccessible(true)
        guavaKt.setAccessible(true)
        val receiver = Fixture("pre-")
        assertEquals(guava.invoke(receiver, "value"), guavaKt.invoke(receiver, "value"))
        assertEquals(
            guava.returning(CharSequence::class.java).invoke(receiver, "value"),
            guavaKt.returning(CharSequence::class).invoke(receiver, "value"),
        )
        assertFailsWith<IllegalArgumentException> { guava.returning(String::class.java) }
        assertFailsWith<IllegalArgumentException> { guavaKt.returning(String::class) }
    }

    @Test
    fun parameterExceptionVarargsAndTypeVariableMetadataMatchGuava() {
        val combine = Fixture::class.java.getDeclaredMethod("combine", String::class.java)
        val guavaParameter = GuavaInvokable.from(combine).parameters.single()
        val guavaKtParameter = GuavaKtInvokable.from(combine).getParameters().single()
        assertEquals(guavaParameter.type.rawType.name, guavaKtParameter.typeName)
        assertEquals(
            guavaParameter.isAnnotationPresent(Marked::class.java),
            guavaKtParameter.isAnnotationPresent(Marked::class),
        )
        assertEquals(0, guavaKtParameter.position)

        val risky = Fixture::class.java.getDeclaredMethod("risky")
        val guavaExceptions = GuavaInvokable.from(risky).exceptionTypes.map { it.rawType.name }
        val guavaKtExceptions = GuavaKtInvokable.from(risky).getExceptionTypes().map { it.getRawType().java.name }
        assertEquals(guavaExceptions, guavaKtExceptions)

        val join = Fixture::class.java.getDeclaredMethod("join", Array<String>::class.java)
        assertEquals(GuavaInvokable.from(join).isVarArgs, GuavaKtInvokable.from(join).isVarArgs())

        val identity = Fixture::class.java.getDeclaredMethod("identity", Any::class.java)
        assertEquals(
            GuavaInvokable.from(identity).typeParameters.map { it.name },
            GuavaKtInvokable.from(identity).getTypeParameters().map { it.name },
        )
    }

    @Test
    fun constructorsAndPrivateAccessibilityMatchGuava() {
        val constructor = Fixture::class.java.getDeclaredConstructor(String::class.java)
        val guava = GuavaInvokable.from(constructor)
        val guavaKt = GuavaKtInvokable.from(constructor)
        assertEquals(guava.isOverridable, guavaKt.isOverridable())
        assertFalse(guavaKt.isOverridable())
        assertEquals(
            guava.isAnnotationPresent(Marked::class.java),
            guavaKt.isAnnotationPresent(Marked::class.java),
        )
        guava.setAccessible(true)
        guavaKt.setAccessible(true)
        assertEquals(guava.invoke(null, "a")!!.combine("b"), guavaKt.invoke(null, "a")?.combine("b"))

        val secret = Fixture::class.java.getDeclaredMethod("secret")
        @Suppress("UNCHECKED_CAST")
        val guavaSecret = GuavaInvokable.from(secret) as GuavaInvokable<Fixture, Any>
        val guavaKtSecret = GuavaKtInvokable.from(secret)
        assertTrue(guavaSecret.trySetAccessible())
        assertTrue(guavaKtSecret.trySetAccessible())
        assertEquals(guavaSecret.invoke(Fixture("x")), guavaKtSecret.invoke(Fixture("x")))
    }
}
