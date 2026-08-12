package com.bernaferrari.guavakt.collect

import kotlin.reflect.KClass

/**
 * An immutable heterogeneous map whose value for each key is an instance of
 * that [KClass].
 *
 * Common Kotlin uses [KClass] instead of Java `Class`. The map snapshots its
 * input, retains insertion order, and validates forged mappings at runtime.
 */
class ImmutableClassToInstanceMap<B : Any> private constructor(
    private val delegate: ImmutableMap<KClass<out B>, B>,
) : AbstractMutableMap<KClass<out B>, B>(), ClassToInstanceMap<B> {
    private val entryView: ImmutableSet<MutableMap.MutableEntry<KClass<out B>, B>> =
        ImmutableSet.copyOf(delegate.entries)
    private val keyView: ImmutableSet<KClass<out B>> = ImmutableSet.copyOf(delegate.keys)
    private val valueView: ImmutableList<B> = ImmutableList.copyOf(delegate.values)

    override val size: Int get() = delegate.size
    override val entries: MutableSet<MutableMap.MutableEntry<KClass<out B>, B>> get() = entryView
    override val keys: MutableSet<KClass<out B>> get() = keyView
    override val values: MutableCollection<B> get() = valueView

    override fun containsKey(key: KClass<out B>): Boolean = delegate.containsKey(key)
    override fun containsValue(value: B): Boolean = delegate.containsValue(value)
    override fun get(key: KClass<out B>): B? = delegate[key]
    override fun isEmpty(): Boolean = delegate.isEmpty()

    override fun <T : B> getInstance(type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate[type] as T?
    }

    override fun <T : B> putInstance(type: KClass<T>, value: T): T? = immutableMutation()
    override fun put(key: KClass<out B>, value: B): B? = immutableMutation()
    override fun putAll(from: Map<out KClass<out B>, B>): Unit = immutableMutation()
    override fun remove(key: KClass<out B>): B? = immutableMutation()
    override fun clear(): Unit = immutableMutation()

    companion object {
        private val EMPTY = ImmutableClassToInstanceMap<Any>(ImmutableMap.of())

        @Suppress("UNCHECKED_CAST")
        fun <B : Any> of(): ImmutableClassToInstanceMap<B> = EMPTY as ImmutableClassToInstanceMap<B>

        fun <B : Any, T : B> of(type: KClass<T>, value: T): ImmutableClassToInstanceMap<B> =
            ImmutableClassToInstanceMap(ImmutableMap.of(type, checked(type, value)))

        @Suppress("UNCHECKED_CAST")
        fun <B : Any> copyOf(map: Map<out KClass<out B>, B>): ImmutableClassToInstanceMap<B> {
            if (map is ImmutableClassToInstanceMap<*>) return map as ImmutableClassToInstanceMap<B>
            return builder<B>().putAll(map).build()
        }

        fun <B : Any> builder(): Builder<B> = Builder()

        /** @deprecated Retained for early GuavaKt source compatibility. */
        fun <B : Any> create(): ImmutableClassToInstanceMap<B> = of()

        /** @deprecated Retained for early GuavaKt source compatibility. */
        fun <B : Any> create(map: Map<out KClass<out B>, B>): ImmutableClassToInstanceMap<B> = copyOf(map)

        internal fun requireClassValue(type: KClass<*>, value: Any) {
            if (!type.isInstance(value)) {
                throw ClassCastException("Value $value is not an instance of $type")
            }
        }

        private fun <T : Any> checked(type: KClass<T>, value: T): T =
            value.also { requireClassValue(type, it) }

        private fun immutableMutation(): Nothing =
            throw UnsupportedOperationException("ImmutableClassToInstanceMap")
    }

    class Builder<B : Any> {
        private val pending = ArrayList<Pair<KClass<out B>, B>>()

        fun <T : B> put(type: KClass<T>, value: T): Builder<B> = apply {
            requireClassValue(type, value)
            pending.add(type to value)
        }

        /**
         * Adds all mappings after checking each value against its runtime key.
         * As in Guava, entries added before a later invalid mapping remain in
         * this reusable builder.
         */
        fun putAll(map: Map<out KClass<out B>, B>): Builder<B> = apply {
            for ((type, value) in map) {
                requireClassValue(type, value)
                pending.add(type to value)
            }
        }

        fun build(): ImmutableClassToInstanceMap<B> {
            if (pending.isEmpty()) return of()
            val mapBuilder = ImmutableMap.builder<KClass<out B>, B>()
            for ((type, value) in pending) mapBuilder.put(type, value)
            return ImmutableClassToInstanceMap(mapBuilder.buildOrThrow())
        }
    }
}
