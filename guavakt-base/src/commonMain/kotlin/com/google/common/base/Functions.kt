package dev.guavakt.base

/** Guava-shaped reusable [Function] factories. Prefer direct Kotlin lambdas for new code. */
object Functions {
    /** Returns the shared pass-through function. */
    @Suppress("UNCHECKED_CAST")
    fun <E> identity(): Function<E, E> = IdentityFunction as Function<E, E>

    fun <E> constant(value: E): Function<Any?, E> = ConstantFunction(value)

    /**
     * Returns a map lookup that throws when [map] has no mapping for its input.
     *
     * A present `null` remains distinct from an absent key, matching Guava's `containsKey` rule.
     */
    fun <K, V> forMap(map: Map<K, V>): Function<K, V> = FunctionForMapNoDefault(map)

    /** Returns a map lookup that uses [defaultValue] only for an absent key. */
    fun <K, V> forMap(map: Map<K, V>, defaultValue: V): Function<K, V> =
        FunctionForMapWithDefault(map, defaultValue)

    fun <A, B, C> compose(g: Function<B, C>, f: Function<A, B>): Function<A, C> =
        FunctionComposition(g, f)

    fun <T> forPredicate(predicate: Predicate<T>): Function<T, Boolean> = PredicateFunction(predicate)

    fun <T> forSupplier(supplier: Supplier<T>): Function<Any?, T> = SupplierFunction(supplier)

    /** Returns the shared function that calls `toString`, rejecting null input. */
    @Suppress("UNCHECKED_CAST")
    fun toStringFunction(): Function<Any?, String> = ToStringFunction as Function<Any?, String>

    private object IdentityFunction : Function<Any?, Any?> {
        override fun apply(input: Any?): Any? = input
        override fun toString(): String = "Functions.identity()"
    }

    private object ToStringFunction : Function<Any?, String> {
        override fun apply(input: Any?): String = Preconditions.checkNotNull(input).toString()
        override fun toString(): String = "Functions.toStringFunction()"
    }

    private class ConstantFunction<E>(private val value: E) : Function<Any?, E> {
        override fun apply(input: Any?): E = value
        override fun equals(other: Any?): Boolean = other is ConstantFunction<*> && value == other.value
        override fun hashCode(): Int = value?.hashCode() ?: 0
        override fun toString(): String = "Functions.constant($value)"
    }

    private class FunctionForMapNoDefault<K, V>(private val map: Map<K, V>) : Function<K, V> {
        @Suppress("UNCHECKED_CAST")
        override fun apply(input: K): V {
            val result = map[input]
            if (result != null || map.containsKey(input)) return result as V
            throw IllegalArgumentException("Key '$input' not present in map")
        }

        override fun equals(other: Any?): Boolean = other is FunctionForMapNoDefault<*, *> && map == other.map
        override fun hashCode(): Int = map.hashCode()
        override fun toString(): String = "Functions.forMap($map)"
    }

    private class FunctionForMapWithDefault<K, V>(
        private val map: Map<K, V>,
        private val defaultValue: V,
    ) : Function<K, V> {
        @Suppress("UNCHECKED_CAST")
        override fun apply(input: K): V {
            val result = map[input]
            return if (result != null || map.containsKey(input)) result as V else defaultValue
        }

        override fun equals(other: Any?): Boolean =
            other is FunctionForMapWithDefault<*, *> && map == other.map && defaultValue == other.defaultValue

        override fun hashCode(): Int = objectsHash(map, defaultValue)
        override fun toString(): String = "Functions.forMap($map, defaultValue=$defaultValue)"
    }

    private class FunctionComposition<A, B, C>(
        private val g: Function<B, C>,
        private val f: Function<A, B>,
    ) : Function<A, C> {
        override fun apply(input: A): C = g.apply(f.apply(input))
        override fun equals(other: Any?): Boolean =
            other is FunctionComposition<*, *, *> && f == other.f && g == other.g

        override fun hashCode(): Int = f.hashCode() xor g.hashCode()
        override fun toString(): String = "$g($f)"
    }

    private class PredicateFunction<T>(private val predicate: Predicate<T>) : Function<T, Boolean> {
        override fun apply(input: T): Boolean = predicate.apply(input)
        override fun equals(other: Any?): Boolean = other is PredicateFunction<*> && predicate == other.predicate
        override fun hashCode(): Int = predicate.hashCode()
        override fun toString(): String = "Functions.forPredicate($predicate)"
    }

    private class SupplierFunction<T>(private val supplier: Supplier<T>) : Function<Any?, T> {
        override fun apply(input: Any?): T = supplier.get()
        override fun equals(other: Any?): Boolean = other is SupplierFunction<*> && supplier == other.supplier
        override fun hashCode(): Int = supplier.hashCode()
        override fun toString(): String = "Functions.forSupplier($supplier)"
    }
}
