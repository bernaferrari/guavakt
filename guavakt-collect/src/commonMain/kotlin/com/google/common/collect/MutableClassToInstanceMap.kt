package dev.guavakt.collect

import kotlin.reflect.KClass

/**
 * A mutable heterogeneous map that enforces each key's runtime class on every value insertion.
 *
 * The optional backing map remains live, matching Guava's ownership-transfer contract. Callers
 * must surrender all other references to it; mutations made through another reference cannot be
 * intercepted. Kotlin nullability intentionally excludes null keys and values.
 */
class MutableClassToInstanceMap<B : Any> private constructor(
    private val backingMap: MutableMap<KClass<out B>, B>,
) : AbstractMutableMap<KClass<out B>, B>(), ClassToInstanceMap<B> {
    private val entrySet = CheckedEntrySet()

    init {
        backingMap.forEach { (type, value) -> requireClassValue(type, value) }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<KClass<out B>, B>>
        get() = entrySet

    override val size: Int
        get() = backingMap.size

    override fun <T : B> getInstance(type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return backingMap[type] as T?
    }

    override fun <T : B> putInstance(type: KClass<T>, value: T): T? {
        @Suppress("UNCHECKED_CAST")
        return put(type, value) as T?
    }

    override fun put(key: KClass<out B>, value: B): B? {
        requireClassValue(key, value)
        return backingMap.put(key, value)
    }

    override fun putAll(from: Map<out KClass<out B>, B>) {
        val snapshot = from.entries.map { it.key to it.value }
        snapshot.forEach { (type, value) -> requireClassValue(type, value) }
        snapshot.forEach { (type, value) -> backingMap[type] = value }
    }

    override fun clear() = backingMap.clear()
    override fun containsKey(key: KClass<out B>): Boolean = backingMap.containsKey(key)
    override fun containsValue(value: B): Boolean = backingMap.containsValue(value)
    override fun get(key: KClass<out B>): B? = backingMap[key]
    override fun isEmpty(): Boolean = backingMap.isEmpty()
    override fun remove(key: KClass<out B>): B? = backingMap.remove(key)

    private inner class CheckedEntrySet : AbstractMutableSet<MutableMap.MutableEntry<KClass<out B>, B>>() {
        override val size: Int
            get() = backingMap.size

        override fun contains(element: MutableMap.MutableEntry<KClass<out B>, B>): Boolean =
            backingMap.entries.contains(element)

        override fun add(element: MutableMap.MutableEntry<KClass<out B>, B>): Boolean =
            throw UnsupportedOperationException("Map entry sets do not support add")

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<KClass<out B>, B>> {
            val iterator = backingMap.entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<KClass<out B>, B>> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableMap.MutableEntry<KClass<out B>, B> = CheckedEntry(iterator.next())
                override fun remove() = iterator.remove()
            }
        }
    }

    private inner class CheckedEntry(
        private val delegate: MutableMap.MutableEntry<KClass<out B>, B>,
    ) : MutableMap.MutableEntry<KClass<out B>, B> {
        override val key: KClass<out B>
            get() = delegate.key
        override val value: B
            get() = delegate.value

        override fun setValue(newValue: B): B {
            requireClassValue(key, newValue)
            return delegate.setValue(newValue)
        }

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
        override fun toString(): String = "$key=$value"
    }

    companion object {
        fun <B : Any> create(): MutableClassToInstanceMap<B> =
            MutableClassToInstanceMap(LinkedHashMap())

        fun <B : Any> create(
            backingMap: MutableMap<KClass<out B>, B>,
        ): MutableClassToInstanceMap<B> = MutableClassToInstanceMap(backingMap)

        private fun requireClassValue(type: KClass<*>, value: Any) =
            ImmutableClassToInstanceMap.requireClassValue(type, value)
    }
}
