package dev.guavakt.primitives

import dev.guavakt.base.Preconditions

/**
 * Static utilities treating Long bits as unsigned 64-bit values. Mirrors Guava UnsignedLongs.
 */
object UnsignedLongs {
    const val MAX_VALUE: Long = -1L // 2^64 - 1

    private fun flip(a: Long): Long = a xor Long.MIN_VALUE

    fun compare(a: Long, b: Long): Int = flip(a).compareTo(flip(b))

    fun min(vararg array: Long): Long {
        Preconditions.checkArgument(array.isNotEmpty())
        var min = flip(array[0])
        for (i in 1 until array.size) {
            val next = flip(array[i])
            if (next < min) min = next
        }
        return flip(min)
    }

    fun max(vararg array: Long): Long {
        Preconditions.checkArgument(array.isNotEmpty())
        var max = flip(array[0])
        for (i in 1 until array.size) {
            val next = flip(array[i])
            if (next > max) max = next
        }
        return flip(max)
    }

    fun join(separator: String, vararg array: Long): String {
        Preconditions.checkNotNull(separator)
        if (array.isEmpty()) return ""
        val sb = StringBuilder(array.size * 12)
        sb.append(toString(array[0]))
        for (i in 1 until array.size) sb.append(separator).append(toString(array[i]))
        return sb.toString()
    }

    fun lexicographicalComparator(): Comparator<LongArray> = LexicographicalComparator.INSTANCE

    private enum class LexicographicalComparator : Comparator<LongArray> {
        INSTANCE;
        override fun compare(a: LongArray, b: LongArray): Int {
            val min = minOf(a.size, b.size)
            for (i in 0 until min) {
                val result = UnsignedLongs.compare(a[i], b[i])
                if (result != 0) return result
            }
            return a.size - b.size
        }
    }

    fun sort(array: LongArray) {
        sort(array, 0, array.size)
    }

    fun sort(array: LongArray, fromIndex: Int, toIndex: Int) {
        Preconditions.checkNotNull(array)
        Preconditions.checkPositionIndexes(fromIndex, toIndex, array.size)
        for (i in fromIndex until toIndex) array[i] = flip(array[i])
        array.sort(fromIndex, toIndex)
        for (i in fromIndex until toIndex) array[i] = flip(array[i])
    }

    fun sortDescending(array: LongArray) {
        sortDescending(array, 0, array.size)
    }

    fun sortDescending(array: LongArray, fromIndex: Int, toIndex: Int) {
        sort(array, fromIndex, toIndex)
        Longs.reverse(array, fromIndex, toIndex)
    }

    fun divide(dividend: Long, divisor: Long): Long {
        if (divisor < 0) { // i.e., divisor >= 2^63
            return if (compare(dividend, divisor) < 0) 0L else 1L
        }
        if (dividend >= 0) return dividend / divisor
        val quotient = (dividend ushr 1) / divisor shl 1
        val rem = dividend - quotient * divisor
        return quotient + if (compare(rem, divisor) >= 0) 1L else 0L
    }

    fun remainder(dividend: Long, divisor: Long): Long {
        if (divisor < 0) {
            return if (compare(dividend, divisor) < 0) dividend else dividend - divisor
        }
        if (dividend >= 0) return dividend % divisor
        val quotient = (dividend ushr 1) / divisor shl 1
        val rem = dividend - quotient * divisor
        return if (compare(rem, divisor) >= 0) rem - divisor else rem
    }

    fun parseUnsignedLong(string: String): Long = parseUnsignedLong(string, 10)

    fun parseUnsignedLong(string: String, radix: Int): Long {
        Preconditions.checkNotNull(string)
        Preconditions.checkArgument(radix in 2..36, "radix (%s) must be between Character.MIN_RADIX and Character.MAX_RADIX", radix)
        if (string.isEmpty()) throw NumberFormatException("empty string")
        val maxSafe = divide(MAX_VALUE, radix.toLong())
        var value = 0L
        for (i in string.indices) {
            val digit = string[i].digitToIntOrNull(radix) ?: throw NumberFormatException(string)
            if (compare(value, maxSafe) > 0 ||
                (value == maxSafe && digit.toLong() > remainder(MAX_VALUE, radix.toLong()))
            ) {
                throw NumberFormatException("Too large for unsigned long: $string")
            }
            value = value * radix + digit
        }
        return value
    }

    fun decode(stringValue: String): Long {
        val request = ParseRequest.fromString(stringValue)
        return try {
            parseUnsignedLong(request.rawValue, request.radix)
        } catch (e: NumberFormatException) {
            throw NumberFormatException("Error parsing value: $stringValue (${e.message})")
        }
    }

    fun toString(x: Long): String = toString(x, 10)

    fun toString(x: Long, radix: Int): String {
        Preconditions.checkArgument(radix in 2..36, "radix (%s) must be between Character.MIN_RADIX and Character.MAX_RADIX", radix)
        if (x == 0L) return "0"
        if (x > 0) return x.toString(radix)
        // Unsigned: use ULong for correct digits
        return x.toULong().toString(radix)
    }
}
