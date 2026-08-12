package com.bernaferrari.guavakt.base

/** Guava FunctionalEquivalence — equivalence on function results. */
class FunctionalEquivalence<F, T>(
    private val function: Function<in F, out T?>,
    private val resultEquivalence: Equivalence<T>,
) : Equivalence<F>() {
    override fun doEquivalent(a: F, b: F): Boolean =
        resultEquivalence.equivalent(function.apply(a), function.apply(b))
    override fun doHash(t: F): Int = resultEquivalence.hash(function.apply(t))

    override fun equals(other: Any?): Boolean =
        other is FunctionalEquivalence<*, *> &&
            function == other.function &&
            resultEquivalence == other.resultEquivalence

    override fun hashCode(): Int = objectsHash(function, resultEquivalence)

    override fun toString(): String = "$resultEquivalence.onResultOf($function)"
}
