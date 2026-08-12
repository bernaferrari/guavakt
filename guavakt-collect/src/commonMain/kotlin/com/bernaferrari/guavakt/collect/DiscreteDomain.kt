package com.bernaferrari.guavakt.collect

import com.bernaferrari.guavakt.math.BigInteger

abstract class DiscreteDomain<C : Comparable<C>> {
    abstract fun next(value: C): C?
    abstract fun previous(value: C): C?
    open fun distance(start: C, end: C): Long = throw UnsupportedOperationException()
    open fun minValue(): C = throw NoSuchElementException()
    open fun maxValue(): C = throw NoSuchElementException()

    companion object {
        /** The singleton domain containing every [Int] value. */
        fun integers(): DiscreteDomain<Int> = IntegerDomain

        /** The singleton domain containing every [Long] value. */
        fun longs(): DiscreteDomain<Long> = LongDomain

        /** The unbounded singleton domain containing every common [BigInteger] value. */
        fun bigIntegers(): DiscreteDomain<BigInteger> = BigIntegerDomain

        private object IntegerDomain : DiscreteDomain<Int>() {
            override fun next(value: Int): Int? = if (value == Int.MAX_VALUE) null else value + 1
            override fun previous(value: Int): Int? = if (value == Int.MIN_VALUE) null else value - 1
            override fun distance(start: Int, end: Int): Long = end.toLong() - start.toLong()
            override fun minValue(): Int = Int.MIN_VALUE
            override fun maxValue(): Int = Int.MAX_VALUE
            override fun toString(): String = "DiscreteDomain.integers()"
        }

        private object LongDomain : DiscreteDomain<Long>() {
            override fun next(value: Long): Long? = if (value == Long.MAX_VALUE) null else value + 1
            override fun previous(value: Long): Long? = if (value == Long.MIN_VALUE) null else value - 1
            override fun distance(start: Long, end: Long): Long {
                val distance = end - start
                return when {
                    end > start && distance < 0L -> Long.MAX_VALUE
                    end < start && distance > 0L -> Long.MIN_VALUE
                    else -> distance
                }
            }
            override fun minValue(): Long = Long.MIN_VALUE
            override fun maxValue(): Long = Long.MAX_VALUE
            override fun toString(): String = "DiscreteDomain.longs()"
        }

        private object BigIntegerDomain : DiscreteDomain<BigInteger>() {
            override fun next(value: BigInteger): BigInteger = value + BigInteger.ONE
            override fun previous(value: BigInteger): BigInteger = value - BigInteger.ONE
            override fun distance(start: BigInteger, end: BigInteger): Long = try {
                (end - start).toLongExact()
            } catch (_: ArithmeticException) {
                if (end >= start) Long.MAX_VALUE else Long.MIN_VALUE
            }
            override fun toString(): String = "DiscreteDomain.bigIntegers()"
        }
    }
}
