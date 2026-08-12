package com.bernaferrari.guavakt.primitives

object UnsignedInts {
    fun toLong(value: Int): Long = value.toLong() and 0xffffffffL
    fun compare(a: Int, b: Int): Int = toLong(a).compareTo(toLong(b))
    fun divide(dividend: Int, divisor: Int): Int = (toLong(dividend) / toLong(divisor)).toInt()
    fun remainder(dividend: Int, divisor: Int): Int = (toLong(dividend) % toLong(divisor)).toInt()
    fun parseUnsignedInt(s: String): Int = parseUnsignedInt(s, 10)
    fun parseUnsignedInt(s: String, radix: Int): Int {
        val l = s.toLong(radix)
        require(l in 0..0xffffffffL)
        return l.toInt()
    }
    fun toString(x: Int): String = toString(x, 10)
    fun toString(x: Int, radix: Int): String = toLong(x).toString(radix)
}
