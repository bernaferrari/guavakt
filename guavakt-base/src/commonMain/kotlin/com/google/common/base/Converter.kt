package dev.guavakt.base

/**
 * A reversible conversion between two non-null Kotlin representations.
 *
 * New Kotlin code will often be clearer with ordinary functions. This type preserves Guava's
 * reusable reverse views, lazy bulk conversion, and compositional value behavior for APIs that
 * benefit from a named bidirectional conversion.
 */
abstract class Converter<A, B> : Function<A, B> {
    private var reverseView: Converter<B, A>? = null

    protected abstract fun doForward(a: A): B
    protected abstract fun doBackward(b: B): A

    /** Converts [a], rejecting a null result as Guava's automatic-null-handling mode does. */
    fun convert(a: A): B = Preconditions.checkNotNull(doForward(a), "Converter.doForward returned null")

    override fun apply(input: A): B = convert(input)

    /** Returns a cached reciprocal view. `converter.reverse().reverse() === converter`. */
    open fun reverse(): Converter<B, A> {
        val cached = reverseView
        if (cached != null) return cached
        return ReverseConverter(this).also { reverseView = it }
    }

    /**
     * Lazily converts each element whenever an iterator is consumed.
     *
     * Kotlin's portable [Iterator] deliberately has no Java `remove`, so this does not expose
     * Guava's JVM iterator-removal bridge.
     */
    fun convertAll(fromIterable: Iterable<A>): Iterable<B> = Iterable {
        val source = fromIterable.iterator()
        object : Iterator<B> {
            override fun hasNext(): Boolean = source.hasNext()
            override fun next(): B = convert(source.next())
        }
    }

    /** Chains this converter with [secondConverter]. */
    open fun <C> andThen(secondConverter: Converter<B, C>): Converter<A, C> =
        ConverterComposition(this, secondConverter)

    private fun convertBackward(b: B): A =
        Preconditions.checkNotNull(doBackward(b), "Converter.doBackward returned null")

    companion object {
        /** Builds a converter from Kotlin functions. Function equality remains Kotlin function identity. */
        fun <A, B> from(forward: (A) -> B, backward: (B) -> A): Converter<A, B> =
            from(Function { forward(it) }, Function { backward(it) })

        /** Builds a converter from reusable GuavaKt [Function] values. */
        fun <A, B> from(
            forwardFunction: Function<in A, out B>,
            backwardFunction: Function<in B, out A>,
        ): Converter<A, B> = FunctionBasedConverter(forwardFunction, backwardFunction)

        /** Returns the shared pass-through converter. */
        @Suppress("UNCHECKED_CAST")
        fun <T> identity(): Converter<T, T> = IdentityConverter as Converter<T, T>
    }

    private class ReverseConverter<A, B>(
        private val original: Converter<B, A>,
    ) : Converter<A, B>() {
        override fun doForward(a: A): B = original.convertBackward(a)
        override fun doBackward(b: B): A = original.convert(b)

        override fun reverse(): Converter<B, A> = original

        override fun equals(other: Any?): Boolean =
            other is ReverseConverter<*, *> && original == other.original

        override fun hashCode(): Int = original.hashCode().inv()

        override fun toString(): String = "$original.reverse()"
    }

    private class ConverterComposition<A, B, C>(
        private val first: Converter<A, B>,
        private val second: Converter<B, C>,
    ) : Converter<A, C>() {
        override fun doForward(a: A): C = second.convert(first.convert(a))
        override fun doBackward(b: C): A = first.convertBackward(second.convertBackward(b))

        override fun equals(other: Any?): Boolean =
            other is ConverterComposition<*, *, *> && first == other.first && second == other.second

        override fun hashCode(): Int = 31 * first.hashCode() + second.hashCode()

        override fun toString(): String = "$first.andThen($second)"
    }

    private class FunctionBasedConverter<A, B>(
        private val forwardFunction: Function<in A, out B>,
        private val backwardFunction: Function<in B, out A>,
    ) : Converter<A, B>() {
        override fun doForward(a: A): B = forwardFunction.apply(a)
        override fun doBackward(b: B): A = backwardFunction.apply(b)

        override fun equals(other: Any?): Boolean =
            other is FunctionBasedConverter<*, *> &&
                forwardFunction == other.forwardFunction &&
                backwardFunction == other.backwardFunction

        override fun hashCode(): Int = 31 * forwardFunction.hashCode() + backwardFunction.hashCode()

        override fun toString(): String = "Converter.from($forwardFunction, $backwardFunction)"
    }

    private object IdentityConverter : Converter<Any?, Any?>() {
        override fun doForward(a: Any?): Any? = a
        override fun doBackward(b: Any?): Any? = b
        override fun reverse(): Converter<Any?, Any?> = this
        override fun <C> andThen(secondConverter: Converter<Any?, C>): Converter<Any?, C> = secondConverter
        override fun toString(): String = "Converter.identity()"
    }
}
