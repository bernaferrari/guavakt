package dev.guavakt.reflect

import java.lang.reflect.AccessibleObject
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

/**
 * JVM-only wrapper around a [Method] or [Constructor].
 *
 * Invocation, accessibility, modifiers, annotations, parameters, exceptions, and varargs retain
 * JDK behavior. Return and owner [TypeToken]s intentionally use GuavaKt's raw-[KClass] model, so
 * parameterized `java.lang.reflect.Type` substitution is not claimed.
 */
abstract class Invokable<T : Any, R : Any> private constructor(
    private val accessibleObject: AccessibleObject,
    private val member: Member,
) {
    fun isAnnotationPresent(annotationClass: Class<out Annotation>): Boolean =
        accessibleObject.isAnnotationPresent(annotationClass)

    fun <A : Annotation> getAnnotation(annotationClass: Class<A>): A? =
        accessibleObject.getAnnotation(annotationClass)

    fun getAnnotations(): Array<Annotation> = accessibleObject.annotations

    fun getDeclaredAnnotations(): Array<Annotation> = accessibleObject.declaredAnnotations

    abstract fun getTypeParameters(): Array<TypeVariable<*>>

    fun setAccessible(flag: Boolean) = accessibleObject.setAccessible(flag)

    fun trySetAccessible(): Boolean =
        try {
            accessibleObject.setAccessible(true)
            true
        } catch (_: Exception) {
            false
        }

    @Suppress("DEPRECATION")
    fun isAccessible(): Boolean = accessibleObject.isAccessible

    fun getName(): String = member.name

    fun getModifiers(): Int = member.modifiers

    fun isSynthetic(): Boolean = member.isSynthetic

    fun isPublic(): Boolean = Modifier.isPublic(member.modifiers)

    fun isProtected(): Boolean = Modifier.isProtected(member.modifiers)

    fun isPackagePrivate(): Boolean = !isPrivate() && !isPublic() && !isProtected()

    fun isPrivate(): Boolean = Modifier.isPrivate(member.modifiers)

    fun isStatic(): Boolean = Modifier.isStatic(member.modifiers)

    fun isFinal(): Boolean = Modifier.isFinal(member.modifiers)

    fun isAbstract(): Boolean = Modifier.isAbstract(member.modifiers)

    fun isNative(): Boolean = Modifier.isNative(member.modifiers)

    fun isSynchronized(): Boolean = Modifier.isSynchronized(member.modifiers)

    abstract fun isOverridable(): Boolean

    abstract fun isVarArgs(): Boolean

    fun invoke(receiver: T?, vararg args: Any?): R? {
        @Suppress("UNCHECKED_CAST")
        return invokeInternal(receiver, args) as R?
    }

    fun getReturnType(): TypeToken<out R> {
        @Suppress("UNCHECKED_CAST")
        return tokenOf(genericReturnType()) as TypeToken<out R>
    }

    fun getParameters(): List<Parameter> {
        val executable = executable()
        val types = visibleGenericParameterTypes()
        val offset = executable.genericParameterTypes.size - types.size
        return types.mapIndexed { position, type ->
            val rawPosition = position + offset
            val reflected = executable.parameters.getOrNull(rawPosition)
            Parameter(
                name = reflected?.takeIf { it.isNamePresent }?.name,
                typeName = type.typeName,
                position = position,
                annotations = executable.parameterAnnotations[rawPosition].toList(),
            )
        }
    }

    fun getExceptionTypes(): List<TypeToken<out Throwable>> =
        executable().genericExceptionTypes.map { type ->
            @Suppress("UNCHECKED_CAST")
            (tokenOf(type) as TypeToken<out Throwable>)
        }

    fun <R1 : Any> returning(returnType: KClass<R1>): Invokable<T, R1> =
        returning(TypeToken.of(returnType))

    fun <R1 : Any> returning(returnType: TypeToken<R1>): Invokable<T, R1> {
        require(returnType.isSupertypeOf(getReturnType())) {
            "Invokable is known to return ${getReturnType()}, not $returnType"
        }
        @Suppress("UNCHECKED_CAST")
        return this as Invokable<T, R1>
    }

    fun getDeclaringClass(): KClass<*> = member.declaringClass.kotlin

    open fun getOwnerType(): TypeToken<T> {
        @Suppress("UNCHECKED_CAST")
        return TypeToken.of(member.declaringClass.kotlin as KClass<T>)
    }

    abstract fun getAnnotatedReturnType(): AnnotatedType

    final override fun equals(other: Any?): Boolean =
        other is Invokable<*, *> && getOwnerType() == other.getOwnerType() && member == other.member

    final override fun hashCode(): Int = member.hashCode()

    final override fun toString(): String = member.toString()

    protected abstract fun invokeInternal(receiver: Any?, args: Array<out Any?>): Any?

    protected abstract fun executable(): Executable

    protected abstract fun genericReturnType(): Type

    protected open fun visibleGenericParameterTypes(): Array<Type> = executable().genericParameterTypes

    private class MethodInvokable(method: Method) : Invokable<Any, Any>(method, method) {
        private val method = method

        override fun getTypeParameters(): Array<TypeVariable<*>> =
            method.typeParameters.map { it as TypeVariable<*> }.toTypedArray()

        override fun isOverridable(): Boolean =
            !(isFinal() || isPrivate() || isStatic() || Modifier.isFinal(method.declaringClass.modifiers))

        override fun isVarArgs(): Boolean = method.isVarArgs

        override fun invokeInternal(receiver: Any?, args: Array<out Any?>): Any? =
            method.invoke(receiver, *args)

        override fun executable(): Executable = method

        override fun genericReturnType(): Type = method.genericReturnType

        override fun getAnnotatedReturnType(): AnnotatedType = method.annotatedReturnType
    }

    private class ConstructorInvokable<T : Any>(constructor: Constructor<T>) :
        Invokable<T, T>(constructor, constructor) {
        private val constructor = constructor

        override fun getTypeParameters(): Array<TypeVariable<*>> =
            (constructor.declaringClass.typeParameters.asList() + constructor.typeParameters.asList())
                .map { it as TypeVariable<*> }
                .toTypedArray()

        override fun isOverridable(): Boolean = false

        override fun isVarArgs(): Boolean = constructor.isVarArgs

        override fun invokeInternal(receiver: Any?, args: Array<out Any?>): Any =
            try {
                constructor.newInstance(*args)
            } catch (failure: InstantiationException) {
                throw RuntimeException("$constructor failed.", failure)
            }

        override fun executable(): Executable = constructor

        override fun genericReturnType(): Type = constructor.declaringClass

        override fun getAnnotatedReturnType(): AnnotatedType = constructor.annotatedReturnType

        override fun visibleGenericParameterTypes(): Array<Type> {
            val types = constructor.genericParameterTypes
            if (types.isEmpty() || !mayNeedHiddenThis()) return types
            val rawTypes = constructor.parameterTypes
            return if (
                types.size == rawTypes.size &&
                rawTypes.firstOrNull() == constructor.declaringClass.enclosingClass
            ) {
                types.copyOfRange(1, types.size)
            } else {
                types
            }
        }

        private fun mayNeedHiddenThis(): Boolean {
            val declaring = constructor.declaringClass
            if (declaring.enclosingConstructor != null) return true
            declaring.enclosingMethod?.let { return !Modifier.isStatic(it.modifiers) }
            return declaring.enclosingClass != null && !Modifier.isStatic(declaring.modifiers)
        }
    }

    companion object {
        fun from(method: Method): Invokable<Any, Any> = MethodInvokable(method)

        fun <T : Any> from(constructor: Constructor<T>): Invokable<T, T> =
            ConstructorInvokable(constructor)

        private fun tokenOf(type: Type): TypeToken<*> {
            val raw = rawClass(type)
            @Suppress("UNCHECKED_CAST")
            return TypeToken.of(raw.kotlin as KClass<Any>)
        }

        private fun rawClass(type: Type): Class<*> =
            when (type) {
                is Class<*> -> type
                is ParameterizedType -> rawClass(type.rawType)
                is TypeVariable<*> -> type.bounds.firstOrNull()?.let(::rawClass) ?: Any::class.java
                is WildcardType -> type.upperBounds.firstOrNull()?.let(::rawClass) ?: Any::class.java
                is GenericArrayType -> java.lang.reflect.Array.newInstance(rawClass(type.genericComponentType), 0).javaClass
                else -> Any::class.java
            }
    }
}
