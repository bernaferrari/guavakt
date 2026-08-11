package dev.guavakt.reflect

/**
 * Guava ImmutableTypeToInstanceMap — read-only type-to-instance map.
 */
class ImmutableTypeToInstanceMap<B : Any> private constructor(
    private val delegate: Map<TypeToken<out B>, B>,
) : TypeToInstanceMap<B>, Map<TypeToken<out B>, B> by delegate {

    override fun <T : B> getInstance(type: TypeToken<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return delegate[type] as T?
    }

    override fun <T : B> getInstance(type: kotlin.reflect.KClass<T>): T? =
        getInstance(TypeToken.of(type))

    fun asMap(): Map<TypeToken<out B>, B> = delegate

    companion object {
        private val EMPTY = ImmutableTypeToInstanceMap<Any>(emptyMap())

        @Suppress("UNCHECKED_CAST")
        fun <B : Any> of(): ImmutableTypeToInstanceMap<B> = EMPTY as ImmutableTypeToInstanceMap<B>

        fun <B : Any> copyOf(map: Map<TypeToken<out B>, B>): ImmutableTypeToInstanceMap<B> {
            if (map.isEmpty()) return of()
            map.forEach { (type, value) -> requireTypeValue(type, value) }
            return ImmutableTypeToInstanceMap(LinkedHashMap(map))
        }

        fun <B : Any> builder(): Builder<B> = Builder()

        fun <B : Any> create(): ImmutableTypeToInstanceMap<B> = of()

        internal fun requireTypeValue(type: TypeToken<*>, value: Any) {
            val rawType = type.getRawType()
            if (!rawType.isInstance(value)) {
                throw ClassCastException("Value $value is not an instance of $rawType")
            }
        }
    }

    class Builder<B : Any> {
        private val map = LinkedHashMap<TypeToken<out B>, B>()
        fun <T : B> put(type: TypeToken<T>, value: T): Builder<B> = apply {
            requireTypeValue(type, value)
            map[type] = value
        }
        fun <T : B> put(type: kotlin.reflect.KClass<T>, value: T): Builder<B> =
            put(TypeToken.of(type), value)
        fun build(): ImmutableTypeToInstanceMap<B> = copyOf(map)
    }
}
