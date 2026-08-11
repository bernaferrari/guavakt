package dev.guavakt.collect

/**
 * Guava CompactHashing — open-addressing helpers for CompactHashMap/Set.
 * Implements smear, mask, and table sizing algorithms from Guava.
 */
internal object CompactHashing {
    const val HASH_FLOODING_FPP = 0.001
    const val MAX_HASH_BUCKET_LENGTH = 9
    const val UNSET: Int = 0
    const val BYTE_MAX_SIZE = 1 shl Byte.SIZE_BITS // 256
    const val BYTE_MASK = (1 shl Byte.SIZE_BITS) - 1
    const val SHORT_MAX_SIZE = 1 shl Short.SIZE_BITS
    const val SHORT_MASK = (1 shl Short.SIZE_BITS) - 1

    fun smear(hashCode: Int): Int {
        // Murmur hash finalizer constants (Guava)
        var h = hashCode * -0x61c88647 // 0xcc9e2d51
        h = (h xor (h ushr 16))
        return h
    }

    fun tableSize(expectedEntries: Int): Int {
        // Next power of two >= expectedEntries, at least 2
        var n = maxOf(expectedEntries, 2)
        n--
        n = n or (n ushr 1)
        n = n or (n ushr 2)
        n = n or (n ushr 4)
        n = n or (n ushr 8)
        n = n or (n ushr 16)
        return n + 1
    }

    fun newTable(size: Int): IntArray = IntArray(size)

    fun remove(
        key: Any?,
        value: Any?,
        mask: Int,
        table: IntArray,
        entries: IntArray,
        keys: Array<Any?>,
        values: Array<Any?>?,
    ): Int {
        // Returns index removed or -1; simplified linear scan on entries arrays
        for (i in keys.indices) {
            if (keys[i] == key && (values == null || values[i] == value)) {
                keys[i] = null
                if (values != null) values[i] = null
                return i
            }
        }
        return -1
    }

    fun getHashPrefix(value: Int, mask: Int): Int = value and mask.inv()
    fun getNext(entry: Int, mask: Int): Int = entry and mask
    fun maskCombine(prefix: Int, next: Int, mask: Int): Int = (prefix and mask.inv()) or (next and mask)
}
