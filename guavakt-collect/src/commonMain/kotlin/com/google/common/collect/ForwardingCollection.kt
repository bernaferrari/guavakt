package dev.guavakt.collect

/**
 * A mutable collection decorator that forwards each operation independently to [delegate].
 *
 * Overriding one operation does not implicitly affect another: for example, overriding [add]
 * does not change [addAll]. Subclasses that want operations composed from their overrides can use
 * the protected `standard*` implementations.
 */
abstract class ForwardingCollection<E> : ForwardingObject(), MutableCollection<E> {
    protected abstract override fun delegate(): MutableCollection<E>

    override val size: Int get() = delegate().size

    override fun iterator(): MutableIterator<E> = delegate().iterator()

    override fun isEmpty(): Boolean = delegate().isEmpty()

    override fun contains(element: E): Boolean = delegate().contains(element)

    override fun add(element: E): Boolean = delegate().add(element)

    override fun remove(element: E): Boolean = delegate().remove(element)

    override fun containsAll(elements: Collection<E>): Boolean = delegate().containsAll(elements)

    override fun addAll(elements: Collection<E>): Boolean = delegate().addAll(elements)

    override fun removeAll(elements: Collection<E>): Boolean = delegate().removeAll(elements)

    override fun retainAll(elements: Collection<E>): Boolean = delegate().retainAll(elements)

    override fun clear() = delegate().clear()

    protected fun standardContains(element: E): Boolean = iterator().asSequence().any { it == element }

    protected fun standardContainsAll(elements: Collection<E>): Boolean = elements.all(::contains)

    protected fun standardAddAll(elements: Collection<E>): Boolean {
        var changed = false
        for (element in elements) changed = add(element) || changed
        return changed
    }

    protected fun standardRemove(element: E): Boolean {
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == element) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    protected fun standardRemoveAll(elements: Collection<E>): Boolean =
        standardRemoveMatching { it in elements }

    protected fun standardRetainAll(elements: Collection<E>): Boolean =
        standardRemoveMatching { it !in elements }

    protected fun standardClear() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    protected fun standardIsEmpty(): Boolean = !iterator().hasNext()

    protected fun standardToString(): String =
        joinToString(prefix = "[", postfix = "]") { element ->
            if (element === this) "(this Collection)" else element.toString()
        }

    private inline fun standardRemoveMatching(predicate: (E) -> Boolean): Boolean {
        var changed = false
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.next())) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }
}
