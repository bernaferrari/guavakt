package dev.guavakt.collect

import dev.guavakt.annotations.GwtCompatible
import dev.guavakt.base.Preconditions

/**
 * Guava ImmutableList — **thin interop shim** over Kotlin's read-only [List].
 *
 * Prefer `listOf(...)` / `emptyList()` / `List` in new Kotlin code. This type exists so
 * Guava-shaped call sites (`ImmutableList.of`, `copyOf`, `builder`) still compile; it
 * does not aim to replicate Guava's specialized immutable array implementations.
 */
@GwtCompatible(serializable = true, emulated = true)
class ImmutableList<E> private constructor(
    private val delegate: List<E>,
) : AbstractMutableList<E>() {
    private var reverseView: ImmutableList<E>? = null

    override val size: Int get() = delegate.size
    override fun get(index: Int): E = delegate[index]
    override fun add(index: Int, element: E): Unit = throw UnsupportedOperationException("ImmutableList")
    override fun addAll(index: Int, elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableList")
    override fun addAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableList")
    override fun remove(element: E): Boolean = throw UnsupportedOperationException("ImmutableList")
    override fun removeAt(index: Int): E = throw UnsupportedOperationException("ImmutableList")
    override fun removeAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableList")
    override fun retainAll(elements: Collection<E>): Boolean = throw UnsupportedOperationException("ImmutableList")
    override fun set(index: Int, element: E): E = throw UnsupportedOperationException("ImmutableList")
    override fun clear(): Unit = throw UnsupportedOperationException("ImmutableList")

    override fun equals(other: Any?): Boolean =
        other === this || (other is List<*> && size == other.size && indices.all { this[it] == other[it] })

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> =
        when {
            fromIndex == 0 && toIndex == size -> this
            fromIndex == toIndex -> of()
            else -> ImmutableList(delegate.subList(fromIndex, toIndex).toList())
        }

    /** Guava convenience; prefer `list.asReversed()` on Kotlin lists. */
    fun reverse(): ImmutableList<E> {
        if (size <= 1) return this
        reverseView?.let { return it }
        val reversed = ImmutableList(delegate.asReversed().toList())
        reversed.reverseView = this
        reverseView = reversed
        return reversed
    }

    class Builder<E>(expectedSize: Int = 4) {
        private val buf = ArrayList<E>(expectedSize.coerceAtLeast(0))
        fun add(element: E): Builder<E> = apply { buf.add(nonNull(element)) }
        fun addAll(elements: Iterable<E>): Builder<E> = apply { for (element in elements) add(element) }
        fun addAll(elements: Iterator<E>): Builder<E> = apply { while (elements.hasNext()) add(elements.next()) }
        fun build(): ImmutableList<E> = copyOf(buf)
    }

    companion object {
        private val EMPTY = ImmutableList<Any>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <E> of(): ImmutableList<E> = EMPTY as ImmutableList<E>

        fun <E> of(element: E): ImmutableList<E> = copyOf(listOf(element))
        fun <E> of(e1: E, e2: E): ImmutableList<E> = copyOf(listOf(e1, e2))
        fun <E> of(e1: E, e2: E, e3: E): ImmutableList<E> = copyOf(listOf(e1, e2, e3))
        fun <E> of(e1: E, e2: E, e3: E, e4: E): ImmutableList<E> = copyOf(listOf(e1, e2, e3, e4))
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableList<E> =
            copyOf(listOf(e1, e2, e3, e4, e5))
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, vararg rest: E): ImmutableList<E> =
            copyOf(listOf(e1, e2, e3, e4, e5, e6, *rest))

        fun <E> copyOf(elements: Collection<E>): ImmutableList<E> {
            if (elements is ImmutableList) return elements
            if (elements.isEmpty()) return of()
            return ImmutableList(elements.map(::nonNull))
        }

        fun <E> copyOf(elements: Iterable<E>): ImmutableList<E> =
            if (elements is Collection) copyOf(elements) else copyOf(elements.toList())

        fun <E> copyOf(elements: Iterator<E>): ImmutableList<E> {
            val list = ArrayList<E>()
            while (elements.hasNext()) list.add(elements.next())
            return copyOf(list)
        }

        fun <E> copyOf(elements: Array<out E>): ImmutableList<E> = copyOf(elements.toList())

        fun <E : Comparable<E>> sortedCopyOf(elements: Iterable<E>): ImmutableList<E> =
            copyOf(elements).let { ImmutableList(it.delegate.sorted()) }

        fun <E> sortedCopyOf(comparator: Comparator<in E>, elements: Iterable<E>): ImmutableList<E> =
            copyOf(elements).let { ImmutableList(it.delegate.sortedWith(comparator)) }

        fun <E> builder(): Builder<E> = Builder()
        fun <E> builderWithExpectedSize(expectedSize: Int): Builder<E> {
            Preconditions.checkArgument(expectedSize >= 0)
            return Builder(expectedSize)
        }

        private fun <T> nonNull(value: T): T = value ?: throw NullPointerException("null element")
    }
}
