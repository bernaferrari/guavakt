package com.bernaferrari.guavakt.base

abstract class Equivalence<T> {
    fun equivalent(a: T?, b: T?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        return doEquivalent(a, b)
    }
    fun hash(t: T?): Int = if (t == null) 0 else doHash(t)
    protected abstract fun doEquivalent(a: T, b: T): Boolean
    protected abstract fun doHash(t: T): Int
    /** Applies this equivalence to results produced by [function]. */
    fun <F> onResultOf(function: Function<in F, out T?>): Equivalence<F> =
        FunctionalEquivalence(function, this)

    fun wrap(reference: T?): Wrapper<T> = Wrapper(this, reference)

    /**
     * Returns a Kotlin predicate for values equivalent to [target].
     *
     * The returned lambda deliberately has Kotlin function identity rather than Guava's
     * value-equality `Predicate` wrapper: Kotlin/JS forbids user implementations of function
     * interfaces, and the function type is the portable Kotlin-first API.
     */
    fun equivalentTo(target: T?): (T?) -> Boolean = { equivalent(it, target) }
    /**
     * Returns element-wise iterable equivalence.
     *
     * Kotlin's covariant [Iterable] makes Guava's Java-only `<S : T>` method type parameter
     * unnecessary: an `Iterable` of a subtype is already accepted where an `Iterable<T>` is used.
     */
    fun pairwise(): Equivalence<Iterable<T>> = PairwiseEquivalence(this)

    companion object {
        /** Returns the shared object-equality strategy, as Guava does. */
        @Suppress("UNCHECKED_CAST")
        fun <T> equals(): Equivalence<T> = EqualsEquivalence as Equivalence<T>

        /**
         * Returns the shared reference-equality strategy.
         *
         * Kotlin common code has no portable `System.identityHashCode`; [hash] therefore uses the
         * object's normal hash code. That may collide for distinct references, which remains valid
         * for an equivalence hash contract.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> identity(): Equivalence<T> = IdentityEquivalence as Equivalence<T>
    }

    class Wrapper<T> internal constructor(
        private val equivalence: Equivalence<in T>,
        private val reference: T?,
    ) {
        fun get(): T? = reference
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Wrapper<*>) return false
            if (equivalence != other.equivalence) return false
            @Suppress("UNCHECKED_CAST")
            return equivalence.equivalent(reference, other.reference as T?)
        }
        override fun hashCode(): Int = equivalence.hash(reference)
        override fun toString(): String = "$equivalence.wrap($reference)"
    }

    private object EqualsEquivalence : Equivalence<Any?>() {
        override fun doEquivalent(a: Any?, b: Any?): Boolean = a == b
        override fun doHash(t: Any?): Int = t.hashCode()
    }

    private object IdentityEquivalence : Equivalence<Any?>() {
        // [equivalent] has already handled the only true case (`a === b`).
        override fun doEquivalent(a: Any?, b: Any?): Boolean = false
        override fun doHash(t: Any?): Int = t.hashCode()
    }
}

internal fun objectsHash(vararg values: Any?): Int {
    var result = 1
    for (value in values) result = 31 * result + (value?.hashCode() ?: 0)
    return result
}
