package dev.guavakt.reflect

import kotlin.reflect.KClass

/**
 * A mutable raw-[TypeToken]-to-instance map for common code.
 *
 * Insertions must use [putInstance]. Direct [put], [putAll], and entry [MutableMap.MutableEntry.setValue]
 * always throw, preserving Guava's type-safety routes. Iterator, key, and value removals remain
 * supported. Tokens are raw [KClass] identities outside the JVM reflection tier.
 */
class MutableTypeToInstanceMap<B : Any> :
    AbstractMutableMap<TypeToken<out B>, B>(),
    TypeToInstanceMap<B> {
    private val backingMap = LinkedHashMap<TypeToken<out B>, B>()
    private val entrySet = SafeEntrySet()

    override val entries: MutableSet<MutableMap.MutableEntry<TypeToken<out B>, B>>
        get() = entrySet

    override val size: Int
        get() = backingMap.size

    override fun <T : B> getInstance(type: TypeToken<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return backingMap[type] as T?
    }

    override fun <T : B> getInstance(type: KClass<T>): T? = getInstance(TypeToken.of(type))

    fun <T : B> putInstance(type: TypeToken<T>, value: T): T? {
        requireTypeValue(type, value)
        @Suppress("UNCHECKED_CAST")
        return backingMap.put(type, value) as T?
    }

    fun <T : B> putInstance(type: KClass<T>, value: T): T? = putInstance(TypeToken.of(type), value)

    @Deprecated("Always throws; use putInstance()", level = DeprecationLevel.WARNING)
    override fun put(key: TypeToken<out B>, value: B): B? = unsupportedPut()

    @Deprecated("Always throws; use putInstance()", level = DeprecationLevel.WARNING)
    override fun putAll(from: Map<out TypeToken<out B>, B>): Unit = unsupportedPut()

    override fun clear() = backingMap.clear()
    override fun containsKey(key: TypeToken<out B>): Boolean = backingMap.containsKey(key)
    override fun containsValue(value: B): Boolean = backingMap.containsValue(value)
    override fun get(key: TypeToken<out B>): B? = backingMap[key]
    override fun isEmpty(): Boolean = backingMap.isEmpty()
    override fun remove(key: TypeToken<out B>): B? = backingMap.remove(key)

    private inner class SafeEntrySet : AbstractMutableSet<MutableMap.MutableEntry<TypeToken<out B>, B>>() {
        override val size: Int
            get() = backingMap.size

        override fun contains(element: MutableMap.MutableEntry<TypeToken<out B>, B>): Boolean =
            backingMap.entries.contains(element)

        override fun add(element: MutableMap.MutableEntry<TypeToken<out B>, B>): Boolean = unsupportedPut()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<TypeToken<out B>, B>> {
            val iterator = backingMap.entries.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<TypeToken<out B>, B>> {
                override fun hasNext(): Boolean = iterator.hasNext()
                override fun next(): MutableMap.MutableEntry<TypeToken<out B>, B> = SafeEntry(iterator.next())
                override fun remove() = iterator.remove()
            }
        }
    }

    private inner class SafeEntry(
        private val delegate: MutableMap.MutableEntry<TypeToken<out B>, B>,
    ) : MutableMap.MutableEntry<TypeToken<out B>, B> {
        override val key: TypeToken<out B>
            get() = delegate.key
        override val value: B
            get() = delegate.value
        override fun setValue(newValue: B): B = unsupportedPut()
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
        override fun toString(): String = "$key=$value"
    }

    companion object {
        fun <B : Any> create(): MutableTypeToInstanceMap<B> = MutableTypeToInstanceMap()

        private fun requireTypeValue(type: TypeToken<*>, value: Any) =
            ImmutableTypeToInstanceMap.requireTypeValue(type, value)

        private fun <T> unsupportedPut(): T =
            throw UnsupportedOperationException("Please use putInstance() instead")
    }
}
