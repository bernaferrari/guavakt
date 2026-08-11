package dev.guavakt.collect

/** Guava collect Platform — KMP uses standard LinkedHashMap / ArrayList. */
internal object Platform {
    private fun mapCapacity(expectedSize: Int): Int {
        if (expectedSize < 3) return expectedSize + 1
        if (expectedSize < (1 shl 30)) return (expectedSize + expectedSize / 3).coerceAtLeast(expectedSize)
        return Int.MAX_VALUE
    }

    fun <K, V> newHashMapWithExpectedSize(expectedSize: Int): MutableMap<K, V> =
        LinkedHashMap(mapCapacity(expectedSize))

    fun <K, V> newLinkedHashMapWithExpectedSize(expectedSize: Int): MutableMap<K, V> =
        LinkedHashMap(mapCapacity(expectedSize))

    fun <E> newArrayListWithExpectedSize(expectedSize: Int): ArrayList<E> =
        ArrayList(expectedSize)

    fun reducesArrayListSize(): Boolean = false
}
