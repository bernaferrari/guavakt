package dev.guavakt.collect

/** Guava ImmutableMapEntry — immutable map entry (not a Map). */
class ImmutableMapEntry<K, V> private constructor(
    override val key: K,
    override val value: V,
) : Map.Entry<K, V> {
    override fun toString(): String = "$key=$value"
    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && other.key == key && other.value == value
    override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)

    companion object {
        fun <K, V> of(key: K, value: V): ImmutableMapEntry<K, V> = ImmutableMapEntry(key, value)
        fun <K, V> create(key: K, value: V): ImmutableMapEntry<K, V> = of(key, value)
    }
}
