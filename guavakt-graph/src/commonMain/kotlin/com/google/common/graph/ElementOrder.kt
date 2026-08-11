package dev.guavakt.graph

import dev.guavakt.collect.ComparatorTreeMap

/**
 * Guava ElementOrder — insertion vs natural vs unordered element iteration.
 */
class ElementOrder<T> private constructor(
    private val type: Type,
    private val comparator: Comparator<T>?,
) {
    enum class Type { UNORDERED, INSERTION, STABLE, SORTED }

    fun type(): Type = type
    fun comparator(): Comparator<T> =
        comparator ?: throw UnsupportedOperationException("This ordering does not define a comparator.")

    fun <T2 : T> cast(): ElementOrder<T2> {
        @Suppress("UNCHECKED_CAST")
        return this as ElementOrder<T2>
    }

    /** The key-identity rule used by maps created from this order. */
    internal fun equivalent(first: T, second: T): Boolean =
        comparator?.compare(first, second) == 0 || comparator == null && first == second

    /**
     * Creates storage whose key identity and iteration match this order.
     *
     * Kotlin common has no `TreeMap`; [ComparatorTreeMap] provides the comparator-equivalence
     * semantics and sorted iteration required by `SORTED` across every supported target.
     */
    fun <V> createMap(expectedSize: Int): MutableMap<T, V> {
        require(expectedSize >= 0) { "expectedSize must be non-negative" }
        return when (type) {
            Type.UNORDERED -> HashMap(expectedSize.coerceAtLeast(16))
            Type.INSERTION, Type.STABLE -> LinkedHashMap(expectedSize.coerceAtLeast(16))
            Type.SORTED -> ComparatorTreeMap(comparator())
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ElementOrder<*> && type == other.type && comparator == other.comparator

    override fun hashCode(): Int = 31 * (31 + type.hashCode()) + (comparator?.hashCode() ?: 0)

    override fun toString(): String =
        if (type == Type.SORTED) {
            "ElementOrder{type=$type, comparator=$comparator}"
        } else {
            "ElementOrder{type=$type}"
        }

    companion object {
        fun <S> unordered(): ElementOrder<S> = ElementOrder(Type.UNORDERED, null)
        fun <S> insertion(): ElementOrder<S> = ElementOrder(Type.INSERTION, null)
        fun <S> stable(): ElementOrder<S> = ElementOrder(Type.STABLE, null)
        fun <S : Comparable<S>> natural(): ElementOrder<S> =
            ElementOrder(Type.SORTED, naturalOrder())
        fun <S> sorted(comparator: Comparator<S>): ElementOrder<S> =
            ElementOrder(Type.SORTED, comparator)
    }
}
