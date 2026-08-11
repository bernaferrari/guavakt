package dev.guavakt.collect

/**
 * Guava Cut — endpoint of a range (below/above a value, or below all / above all).
 */
internal sealed class Cut<C : Comparable<C>> : Comparable<Cut<C>> {
    abstract fun isLessThan(value: C): Boolean
    abstract fun typeAsLowerBound(): BoundType
    abstract fun typeAsUpperBound(): BoundType
    abstract fun describeAsLowerBound(sb: StringBuilder)
    abstract fun describeAsUpperBound(sb: StringBuilder)
    abstract fun leastValueAbove(domain: DiscreteDomain<C>): C?
    abstract fun greatestValueBelow(domain: DiscreteDomain<C>): C?
    open fun withLowerBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> = this
    open fun withUpperBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> = this
    open fun canonical(domain: DiscreteDomain<C>): Cut<C> = this
    abstract val endpoint: C?

    data class BelowValue<C : Comparable<C>>(override val endpoint: C) : Cut<C>() {
        override fun isLessThan(value: C): Boolean = endpoint <= value
        override fun typeAsLowerBound(): BoundType = BoundType.CLOSED
        override fun typeAsUpperBound(): BoundType = BoundType.OPEN
        override fun describeAsLowerBound(sb: StringBuilder) { sb.append('[').append(endpoint) }
        override fun describeAsUpperBound(sb: StringBuilder) { sb.append(endpoint).append(')') }
        override fun leastValueAbove(domain: DiscreteDomain<C>): C = endpoint
        override fun greatestValueBelow(domain: DiscreteDomain<C>): C? = domain.previous(endpoint)
        override fun withLowerBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> =
            if (boundType == BoundType.CLOSED) this else {
                val next = domain.previous(endpoint)
                if (next == null) belowAll() else AboveValue(next)
            }
        override fun withUpperBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> =
            if (boundType == BoundType.OPEN) this else {
                val prev = domain.previous(endpoint)
                if (prev == null) aboveAll() else AboveValue(prev)
            }
        override fun compareTo(other: Cut<C>): Int {
            if (other is BelowAll) return 1
            if (other is AboveAll) return -1
            val result = endpoint.compareTo(other.endpoint!!)
            if (result != 0) return result
            // BelowValue < AboveValue for same endpoint
            return if (other is AboveValue) -1 else 0
        }
        override fun hashCode(): Int = endpoint.hashCode()
    }

    data class AboveValue<C : Comparable<C>>(override val endpoint: C) : Cut<C>() {
        override fun isLessThan(value: C): Boolean = endpoint < value
        override fun typeAsLowerBound(): BoundType = BoundType.OPEN
        override fun typeAsUpperBound(): BoundType = BoundType.CLOSED
        override fun describeAsLowerBound(sb: StringBuilder) { sb.append('(').append(endpoint) }
        override fun describeAsUpperBound(sb: StringBuilder) { sb.append(endpoint).append(']') }
        override fun leastValueAbove(domain: DiscreteDomain<C>): C? = domain.next(endpoint)
        override fun greatestValueBelow(domain: DiscreteDomain<C>): C = endpoint
        override fun withLowerBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> =
            if (boundType == BoundType.OPEN) this else {
                val next = domain.next(endpoint)
                if (next == null) aboveAll() else BelowValue(next)
            }
        override fun withUpperBoundType(boundType: BoundType, domain: DiscreteDomain<C>): Cut<C> =
            if (boundType == BoundType.CLOSED) this else {
                val next = domain.next(endpoint)
                if (next == null) aboveAll() else BelowValue(next)
            }
        override fun canonical(domain: DiscreteDomain<C>): Cut<C> =
            domain.next(endpoint)?.let { BelowValue(it) } ?: aboveAll()
        override fun compareTo(other: Cut<C>): Int {
            if (other is BelowAll) return 1
            if (other is AboveAll) return -1
            val result = endpoint.compareTo(other.endpoint!!)
            if (result != 0) return result
            return if (other is BelowValue) 1 else 0
        }
        override fun hashCode(): Int = endpoint.hashCode().inv()
    }

    class BelowAll<C : Comparable<C>> private constructor() : Cut<C>() {
        override val endpoint: C? get() = null
        override fun isLessThan(value: C): Boolean = true
        override fun typeAsLowerBound(): BoundType = BoundType.OPEN
        override fun typeAsUpperBound(): BoundType = error("not supported")
        override fun describeAsLowerBound(sb: StringBuilder) { sb.append("(-∞") }
        override fun describeAsUpperBound(sb: StringBuilder) { error("not supported") }
        override fun leastValueAbove(domain: DiscreteDomain<C>): C? = domain.minValue()
        override fun greatestValueBelow(domain: DiscreteDomain<C>): C? = error("not supported")
        override fun compareTo(other: Cut<C>): Int = if (other is BelowAll) 0 else -1
        override fun hashCode(): Int = this::class.hashCode()
        override fun toString(): String = "-∞"
        companion object {
            private val INSTANCE = BelowAll<Comparable<Any>>()
            @Suppress("UNCHECKED_CAST")
            fun <C : Comparable<C>> instance(): Cut<C> = INSTANCE as Cut<C>
        }
    }

    class AboveAll<C : Comparable<C>> private constructor() : Cut<C>() {
        override val endpoint: C? get() = null
        override fun isLessThan(value: C): Boolean = false
        override fun typeAsLowerBound(): BoundType = error("not supported")
        override fun typeAsUpperBound(): BoundType = BoundType.OPEN
        override fun describeAsLowerBound(sb: StringBuilder) { error("not supported") }
        override fun describeAsUpperBound(sb: StringBuilder) { sb.append("+∞)") }
        override fun leastValueAbove(domain: DiscreteDomain<C>): C? = error("not supported")
        override fun greatestValueBelow(domain: DiscreteDomain<C>): C? = domain.maxValue()
        override fun compareTo(other: Cut<C>): Int = if (other is AboveAll) 0 else 1
        override fun hashCode(): Int = this::class.hashCode()
        override fun toString(): String = "+∞"
        companion object {
            private val INSTANCE = AboveAll<Comparable<Any>>()
            @Suppress("UNCHECKED_CAST")
            fun <C : Comparable<C>> instance(): Cut<C> = INSTANCE as Cut<C>
        }
    }

    companion object {
        fun <C : Comparable<C>> belowAll(): Cut<C> = BelowAll.instance()
        fun <C : Comparable<C>> aboveAll(): Cut<C> = AboveAll.instance()
        fun <C : Comparable<C>> belowValue(endpoint: C): Cut<C> = BelowValue(endpoint)
        fun <C : Comparable<C>> aboveValue(endpoint: C): Cut<C> = AboveValue(endpoint)
    }
}
