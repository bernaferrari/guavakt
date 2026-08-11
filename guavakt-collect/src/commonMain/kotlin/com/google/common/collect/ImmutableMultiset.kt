package dev.guavakt.collect

/**
 * An immutable, first-occurrence-ordered snapshot of a [Multiset].
 *
 * Null elements are rejected at construction time, matching Guava. New Kotlin
 * code can usually use a `Map<E, Int>` when only counts are needed; this type is
 * useful when repeated-element iteration and Guava-shaped multiset operations
 * are part of the contract.
 */
class ImmutableMultiset<E> private constructor(
    private val backing: LinkedHashMultiset<E>,
) : AbstractMultiset<E>() {
    private val elements: ImmutableSet<E> = ImmutableSet.copyOf(backing.elementSet())
    private val entries: ImmutableSet<Multiset.Entry<E>> = ImmutableSet.copyOf(backing.entrySet())
    private val list: ImmutableList<E> by lazy { ImmutableList.copyOf(this) }

    override val size: Int get() = backing.size

    override fun iterator(): MutableIterator<E> {
        val iterator = backing.iterator()
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E = iterator.next()
            override fun remove(): Nothing = immutableMutation()
        }
    }

    /** Returns the repeated-element iteration order as an immutable list. */
    fun asList(): ImmutableList<E> = list

    override fun count(element: Any?): Int = backing.count(element)
    override fun contains(element: E): Boolean = backing.count(element) > 0
    override fun elementSet(): ImmutableSet<E> = elements
    override fun entrySet(): ImmutableSet<Multiset.Entry<E>> = entries

    override fun add(element: E): Boolean = immutableMutation()
    override fun add(element: E, occurrences: Int): Int = immutableMutation()
    override fun addAll(elements: Collection<E>): Boolean = immutableMutation()
    override fun remove(element: E): Boolean = immutableMutation()
    override fun remove(element: Any?, occurrences: Int): Int = immutableMutation()
    override fun removeAll(elements: Collection<E>): Boolean = immutableMutation()
    override fun retainAll(elements: Collection<E>): Boolean = immutableMutation()
    override fun setCount(element: E, count: Int): Int = immutableMutation()
    override fun setCount(element: E, oldCount: Int, newCount: Int): Boolean = immutableMutation()
    override fun clear(): Nothing = immutableMutation()

    companion object {
        private val EMPTY = ImmutableMultiset<Any>(LinkedHashMultiset.create())

        @Suppress("UNCHECKED_CAST")
        fun <E> of(): ImmutableMultiset<E> = EMPTY as ImmutableMultiset<E>

        fun <E> of(element: E): ImmutableMultiset<E> = copyOf(listOf(element))
        fun <E> of(e1: E, e2: E): ImmutableMultiset<E> = copyOf(listOf(e1, e2))
        fun <E> of(e1: E, e2: E, e3: E): ImmutableMultiset<E> = copyOf(listOf(e1, e2, e3))
        fun <E> of(e1: E, e2: E, e3: E, e4: E): ImmutableMultiset<E> =
            copyOf(listOf(e1, e2, e3, e4))
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E): ImmutableMultiset<E> =
            copyOf(listOf(e1, e2, e3, e4, e5))
        fun <E> of(e1: E, e2: E, e3: E, e4: E, e5: E, e6: E, vararg others: E): ImmutableMultiset<E> =
            copyOf(listOf(e1, e2, e3, e4, e5, e6, *others))

        @Suppress("UNCHECKED_CAST")
        fun <E> copyOf(elements: Iterable<out E>): ImmutableMultiset<E> {
            if (elements is ImmutableMultiset<*>) return elements as ImmutableMultiset<E>
            if (elements is Multiset<*>) return copyOfMultiset(elements as Multiset<out E>)

            val snapshot = LinkedHashMultiset.create<E>()
            for (element in elements) snapshot.add(nonNull(element))
            return fromSnapshot(snapshot)
        }

        fun <E> copyOf(elements: Iterator<out E>): ImmutableMultiset<E> {
            val snapshot = LinkedHashMultiset.create<E>()
            while (elements.hasNext()) snapshot.add(nonNull(elements.next()))
            return fromSnapshot(snapshot)
        }

        fun <E> copyOf(elements: Array<out E>): ImmutableMultiset<E> = copyOf(elements.asList())

        fun <E> create(): ImmutableMultiset<E> = of()
        fun <E> create(elements: Iterable<out E>): ImmutableMultiset<E> = copyOf(elements)

        fun <E> builder(): Builder<E> = Builder()

        private fun <E> copyOfMultiset(source: Multiset<out E>): ImmutableMultiset<E> {
            val snapshot = LinkedHashMultiset.create<E>(source.elementSet().size)
            for (entry in source.entrySet()) {
                snapshot.add(nonNull(entry.getElement()), entry.getCount())
            }
            return fromSnapshot(snapshot)
        }

        private fun <E> fromSnapshot(snapshot: LinkedHashMultiset<E>): ImmutableMultiset<E> =
            if (snapshot.isEmpty()) of() else ImmutableMultiset(snapshot)

        private fun <T> nonNull(value: T): T = value ?: throw NullPointerException("null element")
        private fun immutableMutation(): Nothing = throw UnsupportedOperationException("ImmutableMultiset")
    }

    class Builder<E> {
        private val contents = LinkedHashMultiset.create<E>()

        fun add(element: E): Builder<E> = apply { contents.add(nonNull(element)) }
        fun add(vararg elements: E): Builder<E> = apply { elements.forEach(::add) }
        fun addCopies(element: E, occurrences: Int): Builder<E> = apply {
            contents.add(nonNull(element), occurrences)
        }
        fun setCount(element: E, count: Int): Builder<E> = apply {
            contents.setCount(nonNull(element), count)
        }
        fun addAll(elements: Iterable<out E>): Builder<E> = apply {
            if (elements is Multiset<*>) {
                @Suppress("UNCHECKED_CAST")
                for (entry in (elements as Multiset<out E>).entrySet()) {
                    contents.add(nonNull(entry.getElement()), entry.getCount())
                }
            } else {
                for (element in elements) add(element)
            }
        }
        fun addAll(elements: Iterator<out E>): Builder<E> = apply {
            while (elements.hasNext()) add(elements.next())
        }

        /** Builds a snapshot; later builder changes never affect earlier results. */
        fun build(): ImmutableMultiset<E> = copyOfMultiset(contents)
    }
}
