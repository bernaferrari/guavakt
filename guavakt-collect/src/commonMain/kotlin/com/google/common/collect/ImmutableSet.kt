package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions


/**
 * Guava ImmutableSet — **thin interop shim** over Kotlin read-only collections.
 * Prefer `setOf(...) / emptySet() / Set` in new Kotlin code; factories kept for Guava-shaped call sites only.
 */
@GwtCompatible(serializable = true, emulated = true)
class ImmutableSet<E> private constructor(
    private val delegate: Set<E>,
) : AbstractMutableSet<E>() {
    override val size: Int get() = delegate.size
    override fun contains(element: E): Boolean = delegate.contains(element)
    override fun iterator(): MutableIterator<E> {
        val iterator = delegate.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next()
            override fun remove(): Unit = throw UnsupportedOperationException("ImmutableSet")
        }
    }
    override fun add(element: E): Boolean = throw UnsupportedOperationException("ImmutableSet")
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableSet")
    override fun remove(element: E): Boolean = throw UnsupportedOperationException("ImmutableSet")
    override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableSet")
    override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableSet")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableSet")

    override fun equals(other: Any?): Boolean =
        other === this || (other is Set<*> && size == other.size && other.all { it in delegate })

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    fun asList(): ImmutableList<E> = ImmutableList.copyOf(delegate)

    companion object {
        private val EMPTY = ImmutableSet<Any>(emptySet())

        @Suppress("UNCHECKED_CAST")
        fun <E> of(): ImmutableSet<E> = EMPTY as ImmutableSet<E>

        fun <E> of(element: E): ImmutableSet<E> = copyOf(listOf(element))

        fun <E> of(e1: E, e2: E): ImmutableSet<E> = copyOf(listOf(e1, e2))

        fun <E> of(e1: E, e2: E, e3: E): ImmutableSet<E> = copyOf(listOf(e1, e2, e3))

        fun <E> of(e1: E, e2: E, e3: E, e4: E): ImmutableSet<E> = copyOf(listOf(e1, e2, e3, e4))

        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableSet<E> =
            copyOf(listOf(e1, e2, e3, e4, e5))

        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, vararg others: E): ImmutableSet<E> {
            val list = ArrayList<E>(6 + others.size)
            list.add(e1); list.add(e2); list.add(e3); list.add(e4); list.add(e5); list.add(e6)
            others.forEach { list.add(it) }
            return copyOf(list)
        }

        fun <E> copyOf(elements: Collection<E>): ImmutableSet<E> {
            if (elements is ImmutableSet) return elements
            if (elements.isEmpty()) return of()
            // preserve insertion order, drop duplicates
            val snapshot = LinkedHashSet<E>()
            for (element in elements) snapshot.add(nonNull(element))
            return ImmutableSet(snapshot)
        }

        fun <E> copyOf(elements: Iterable<E>): ImmutableSet<E> =
            if (elements is Collection) copyOf(elements) else copyOf(elements.toList())

        fun <E> copyOf(elements: Iterator<E>): ImmutableSet<E> {
            if (!elements.hasNext()) return of()
            val first = elements.next()
            if (!elements.hasNext()) return of(first)
            return builder<E>().add(first).addAll(Iterable { elements }).build()
        }

        fun <E> copyOf(elements: Array<out E>): ImmutableSet<E> = copyOf(elements.asList())

        fun <E> builder(): Builder<E> = Builder()

        fun <E> builderWithExpectedSize(expectedSize: Int): Builder<E> {
            Preconditions.checkArgument(expectedSize >= 0)
            return Builder(expectedSize)
        }

        private fun <T> nonNull(value: T): T = value ?: throw NullPointerException("null element")
    }

    class Builder<E>(expectedSize: Int = 4) {
        private val set = LinkedHashSet<E>(expectedSize.coerceAtLeast(4))
        fun add(element: E): Builder<E> = apply { set.add(nonNull(element)) }
        fun add(vararg elements: E): Builder<E> = apply { elements.forEach { add(it) } }
        fun addAll(elements: Iterable<E>): Builder<E> = apply { elements.forEach { add(it) } }
        fun addAll(elements: Iterator<E>): Builder<E> = apply {
            while (elements.hasNext()) add(elements.next())
        }
        fun build(): ImmutableSet<E> =
            if (set.isEmpty()) of() else ImmutableSet(LinkedHashSet(set))
    }

}
