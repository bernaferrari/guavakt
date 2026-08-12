package com.bernaferrari.guavakt.math

import kotlin.math.ln
import kotlin.random.Random

/**
 * Immutable arbitrary-precision signed integer for common Kotlin.
 *
 * Limbs are base 10⁹, making formatting and decimal parsing predictable on every target. This is
 * intentionally a common KMP numeric value rather than a facade over `java.math.BigInteger`.
 *
 * The arithmetic, radix, signed-byte, modular, prime, and two's-complement operations follow
 * `java.math.BigInteger`'s value semantics. JVM serialization, native JDK interop, and the JDK's
 * allocation/performance strategy are deliberately outside this portable type's contract.
 */
class BigInteger private constructor(
    private val sign: Int,
    private val limbs: IntArray,
) : Comparable<BigInteger> {
    val signum: Int get() = sign
    val isZero: Boolean get() = sign == 0

    operator fun unaryMinus(): BigInteger = if (isZero) this else BigInteger(-sign, limbs)

    /** Java-shaped alias for migration call sites. */
    fun negate(): BigInteger = -this

    /** Java-shaped function form, alongside Kotlin's [signum] property. */
    fun signum(): Int = sign

    fun abs(): BigInteger = if (sign >= 0) this else -this

    operator fun plus(other: BigInteger): BigInteger {
        if (isZero) return other
        if (other.isZero) return this
        if (sign == other.sign) return ofMagnitude(sign, addMagnitude(limbs, other.limbs))
        val comparison = compareMagnitude(limbs, other.limbs)
        return when {
            comparison == 0 -> ZERO
            comparison > 0 -> ofMagnitude(sign, subtractMagnitude(limbs, other.limbs))
            else -> ofMagnitude(other.sign, subtractMagnitude(other.limbs, limbs))
        }
    }

    /** Java-shaped alias for Kotlin's `+`. */
    fun add(other: BigInteger): BigInteger = this + other

    operator fun plus(other: Long): BigInteger = this + of(other)

    operator fun minus(other: BigInteger): BigInteger = this + -other

    /** Java-shaped alias for Kotlin's `-`. */
    fun subtract(other: BigInteger): BigInteger = this - other

    operator fun minus(other: Long): BigInteger = this - of(other)

    operator fun times(other: BigInteger): BigInteger {
        if (isZero || other.isZero) return ZERO
        return ofMagnitude(sign * other.sign, multiplyMagnitude(limbs, other.limbs))
    }

    /** Java-shaped alias for Kotlin's `*`. */
    fun multiply(other: BigInteger): BigInteger = this * other

    operator fun times(other: Int): BigInteger {
        if (other == 0 || isZero) return ZERO
        val multiplier = if (other < 0) -other.toLong() else other.toLong()
        val result = multiplyMagnitudeByLong(limbs, multiplier)
        return ofMagnitude(if (other < 0) -sign else sign, result)
    }

    operator fun times(other: Long): BigInteger = this * of(other)

    /** Quotient rounded toward zero. */
    operator fun div(other: BigInteger): BigInteger = divideAndRemainder(other).first

    /** Java-shaped alias for Kotlin's `/`. */
    fun divide(other: BigInteger): BigInteger = this / other

    operator fun rem(other: BigInteger): BigInteger = divideAndRemainder(other).second

    /** Java-shaped alias for Kotlin's `%`. */
    fun remainder(other: BigInteger): BigInteger = this % other

    fun divideAndRemainder(other: BigInteger): Pair<BigInteger, BigInteger> {
        if (other.isZero) throw ArithmeticException("division by zero")
        if (isZero) return ZERO to ZERO
        val magnitudeResult = divideMagnitude(limbs, other.limbs)
        val quotient = ofMagnitude(sign * other.sign, magnitudeResult.first)
        val remainder = ofMagnitude(sign, magnitudeResult.second)
        return quotient to remainder
    }

    /** Positive modulo, matching `java.math.BigInteger.mod`. */
    fun mod(modulus: BigInteger): BigInteger {
        require(modulus.sign > 0) { "modulus must be positive" }
        val remainder = this % modulus
        return if (remainder.sign >= 0) remainder else remainder + modulus
    }

    fun pow(exponent: Int): BigInteger {
        require(exponent >= 0) { "negative exponent: $exponent" }
        var power = exponent
        var base = this
        var result = ONE
        while (power != 0) {
            if ((power and 1) != 0) result *= base
            power = power ushr 1
            if (power != 0) base *= base
        }
        return result
    }

    /**
     * Arithmetic left shift. A negative distance follows Java's reciprocal-shift convention.
     */
    fun shiftLeft(bits: Int): BigInteger {
        if (bits < 0) {
            if (bits == Int.MIN_VALUE) throw ArithmeticException("shift distance out of range")
            return shiftRight(-bits)
        }
        return if (bits == 0 || isZero) this else this * TWO.pow(bits)
    }

    /**
     * Arithmetic right shift with infinite sign extension. A negative distance follows Java's
     * reciprocal-shift convention.
     */
    fun shiftRight(bits: Int): BigInteger {
        if (bits < 0) {
            if (bits == Int.MIN_VALUE) throw ArithmeticException("shift distance out of range")
            return shiftLeft(-bits)
        }
        if (bits == 0 || isZero) return this
        if (sign > 0) return if (bits >= positiveBitLength()) ZERO else abs() / TWO.pow(bits)
        if (bits >= bitLength() + 1) return MINUS_ONE
        val divisor = TWO.pow(bits)
        // Java right-shifts negatives toward negative infinity, not toward zero.
        return -((abs() + divisor - ONE) / divisor)
    }

    /**
     * Number of bits in Java's minimal two's-complement representation, excluding the sign bit.
     */
    fun bitLength(): Int {
        if (isZero) return 0
        return if (sign > 0) positiveBitLength() else (-this - ONE).positiveBitLength()
    }

    /** Number of one bits in the finite two's-complement representation. */
    fun bitCount(): Int {
        val magnitude = if (sign >= 0) abs() else -this - ONE
        var remaining = magnitude
        var count = 0
        while (!remaining.isZero) {
            if ((remaining % TWO) == ONE) count++
            remaining /= TWO
        }
        return count
    }

    /** Lowest set bit, or `-1` for zero. */
    fun getLowestSetBit(): Int {
        if (isZero) return -1
        var remaining = if (sign > 0) this else -this
        var bits = 0
        while ((remaining % TWO).isZero) {
            remaining /= TWO
            bits++
        }
        return bits
    }

    fun testBit(bit: Int): Boolean {
        val checked = checkedBit(bit)
        val width = maxOf(bitLength() + 2, checked + 2)
        return toTwosComplement(width)[checked]
    }

    fun setBit(bit: Int): BigInteger = binaryOperation(TWO.pow(checkedBit(bit))) { first, second -> first || second }

    fun clearBit(bit: Int): BigInteger = binaryOperation(TWO.pow(checkedBit(bit))) { first, second -> first && !second }

    fun flipBit(bit: Int): BigInteger = binaryOperation(TWO.pow(checkedBit(bit))) { first, second -> first != second }

    infix fun and(other: BigInteger): BigInteger = binaryOperation(other) { first, second -> first && second }

    infix fun or(other: BigInteger): BigInteger = binaryOperation(other) { first, second -> first || second }

    infix fun xor(other: BigInteger): BigInteger = binaryOperation(other) { first, second -> first != second }

    fun not(): BigInteger = -this - ONE

    fun andNot(other: BigInteger): BigInteger = binaryOperation(other) { first, second -> first && !second }

    private fun positiveBitLength(): Int {
        if (isZero) return 0
        val magnitude = abs()
        val top = limbs.last().toDouble()
        var floor = (((limbs.size - 1) * LOG2_BASE) + ln(top) / LN_2).toInt()
        var lowerPower = TWO.pow(floor)
        while (magnitude < lowerPower) {
            floor--
            lowerPower /= TWO
        }
        while (magnitude >= lowerPower * TWO) {
            floor++
            lowerPower *= TWO
        }
        return floor + 1
    }

    fun isPowerOfTwo(): Boolean = sign > 0 && bitCount() == 1

    /** Greatest common divisor, always non-negative (including `gcd(0, 0) == 0`). */
    fun gcd(other: BigInteger): BigInteger {
        var first = abs()
        var second = other.abs()
        while (!second.isZero) {
            val next = first % second
            first = second
            second = next
        }
        return first
    }

    fun toIntExact(): Int {
        val result = toLongExact()
        if (result !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            throw ArithmeticException("BigInteger out of Int range")
        }
        return result.toInt()
    }

    fun toLongExact(): Long {
        if (sign > 0 && this > of(Long.MAX_VALUE)) throw ArithmeticException("BigInteger out of Long range")
        if (sign < 0 && abs() > MIN_LONG_MAGNITUDE) throw ArithmeticException("BigInteger out of Long range")
        if (this == -MIN_LONG_MAGNITUDE) return Long.MIN_VALUE
        var value = 0L
        for (index in limbs.lastIndex downTo 0) {
            if (value > Long.MAX_VALUE / BASE) throw ArithmeticException("BigInteger out of Long range")
            value *= BASE
            if (value > Long.MAX_VALUE - limbs[index]) throw ArithmeticException("BigInteger out of Long range")
            value += limbs[index].toLong()
        }
        return if (sign < 0) -value else value
    }

    /** Low 32 bits interpreted as a signed `Int`, matching `java.math.BigInteger.intValue`. */
    fun toInt(): Int = toLong().toInt()

    /** Low 64 bits interpreted as a signed `Long`, matching `java.math.BigInteger.longValue`. */
    fun toLong(): Long {
        var value = 0UL
        for (index in limbs.lastIndex downTo 0) {
            value = value * BASE.toULong() + limbs[index].toULong()
        }
        return if (sign < 0) -value.toLong() else value.toLong()
    }

    fun toShort(): Short = toInt().toShort()

    fun toByte(): Byte = toInt().toByte()

    fun toShortExact(): Short {
        val value = toLongExact()
        if (value !in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()) {
            throw ArithmeticException("BigInteger out of Short range")
        }
        return value.toShort()
    }

    fun toByteExact(): Byte {
        val value = toLongExact()
        if (value !in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()) {
            throw ArithmeticException("BigInteger out of Byte range")
        }
        return value.toByte()
    }

    fun toDouble(): Double = toString().toDouble()

    fun toFloat(): Float = toString().toFloat()

    fun min(other: BigInteger): BigInteger = if (this <= other) this else other

    fun max(other: BigInteger): BigInteger = if (this >= other) this else other

    /** Integer square root rounded down. */
    fun sqrt(): BigInteger = BigIntegerMath.sqrt(this, RoundingMode.FLOOR)

    fun sqrtAndRemainder(): Pair<BigInteger, BigInteger> {
        val root = sqrt()
        return root to (this - root * root)
    }

    /** Modular exponentiation for a strictly positive modulus. */
    fun modPow(exponent: BigInteger, modulus: BigInteger): BigInteger {
        require(modulus.sign > 0) { "modulus must be positive" }
        require(exponent.sign >= 0) { "negative exponent" }
        var power = exponent
        var base = mod(modulus)
        var result = ONE.mod(modulus)
        while (!power.isZero) {
            if ((power % TWO) == ONE) result = (result * base).mod(modulus)
            power /= TWO
            if (!power.isZero) base = (base * base).mod(modulus)
        }
        return result
    }

    /** Modular multiplicative inverse for a strictly positive modulus. */
    fun modInverse(modulus: BigInteger): BigInteger {
        require(modulus.sign > 0) { "modulus must be positive" }
        var previousRemainder = modulus
        var remainder = mod(modulus)
        var previousCoefficient = ZERO
        var coefficient = ONE
        while (!remainder.isZero) {
            val quotient = previousRemainder / remainder
            val nextRemainder = previousRemainder - quotient * remainder
            previousRemainder = remainder
            remainder = nextRemainder
            val nextCoefficient = previousCoefficient - quotient * coefficient
            previousCoefficient = coefficient
            coefficient = nextCoefficient
        }
        if (previousRemainder != ONE) throw ArithmeticException("BigInteger not invertible")
        return previousCoefficient.mod(modulus)
    }

    /** Miller-Rabin probable-prime check with deterministic small-prime witnesses. */
    fun isProbablePrime(certainty: Int): Boolean {
        if (certainty <= 0) return true
        if (sign <= 0 || this == ONE) return false
        if (this == TWO || this == THREE) return true
        if ((this % TWO).isZero) return false
        for (prime in SMALL_PRIMES) {
            val divisor = of(prime)
            if (this == divisor) return true
            if ((this % divisor).isZero) return false
        }

        var oddPart = this - ONE
        var shifts = 0
        while ((oddPart % TWO).isZero) {
            oddPart /= TWO
            shifts++
        }
        val rounds = minOf(SMALL_PRIMES.size, maxOf(1, (certainty + 1) / 2))
        for (index in 0 until rounds) {
            val witness = of(SMALL_PRIMES[index]).mod(this - THREE) + TWO
            var power = witness.modPow(oddPart, this)
            if (power == ONE || power == this - ONE) continue
            var passed = false
            repeat(shifts - 1) {
                power = power.modPow(TWO, this)
                if (power == this - ONE) passed = true
            }
            if (!passed) return false
        }
        return true
    }

    /** Smallest prime strictly larger than this value. */
    fun nextProbablePrime(): BigInteger {
        var candidate = when {
            this < TWO -> TWO
            this == TWO -> THREE
            else -> this + ONE
        }
        if ((candidate % TWO).isZero && candidate != TWO) candidate += ONE
        while (!candidate.isProbablePrime(100)) candidate += TWO
        return candidate
    }

    override fun compareTo(other: BigInteger): Int {
        if (sign != other.sign) return sign.compareTo(other.sign)
        if (sign == 0) return 0
        val comparison = compareMagnitude(limbs, other.limbs)
        return if (sign > 0) comparison else -comparison
    }

    override fun equals(other: Any?): Boolean =
        other is BigInteger && sign == other.sign && limbs.contentEquals(other.limbs)

    override fun hashCode(): Int {
        var result = sign
        for (limb in limbs) result = 31 * result + limb
        return result
    }

    /** Minimal signed big-endian two's-complement byte representation. */
    fun toByteArray(): ByteArray {
        if (isZero) return byteArrayOf(0)
        val width = if (sign > 0) (positiveBitLength() + 1 + 7) / 8 else (bitLength() + 1 + 7) / 8
        val encoded = if (sign > 0) this else TWO.pow(width * 8) + this
        return encoded.toUnsignedByteArray(width)
    }

    /** Text representation in a radix from 2 through 36. */
    fun toString(radix: Int): String {
        require(radix in 2..36) { "radix out of range: $radix" }
        if (isZero) return "0"
        var value = abs()
        val digits = StringBuilder()
        val base = of(radix)
        while (!value.isZero) {
            val (quotient, remainder) = value.divideAndRemainder(base)
            digits.append(DIGITS[remainder.toIntExact()])
            value = quotient
        }
        if (sign < 0) digits.append('-')
        return digits.reverse().toString()
    }

    override fun toString(): String {
        if (isZero) return "0"
        return buildString(limbs.size * 9 + 1) {
            if (sign < 0) append('-')
            append(limbs.last())
            for (index in limbs.lastIndex - 1 downTo 0) {
                val limb = limbs[index].toString()
                repeat(9 - limb.length) { append('0') }
                append(limb)
            }
        }
    }

    private fun binaryOperation(other: BigInteger, operation: (Boolean, Boolean) -> Boolean): BigInteger {
        val width = maxOf(bitLength(), other.bitLength()) + 2
        val first = toTwosComplement(width)
        val second = other.toTwosComplement(width)
        val result = BooleanArray(width) { index -> operation(first[index], second[index]) }
        return fromTwosComplement(result)
    }

    private fun toTwosComplement(width: Int): BooleanArray {
        require(width > 0) { "two's-complement width must be positive" }
        var value = if (sign >= 0) this else TWO.pow(width) + this
        val result = BooleanArray(width)
        for (index in 0 until width) {
            result[index] = (value % TWO) == ONE
            value /= TWO
        }
        return result
    }

    private fun fromTwosComplement(bits: BooleanArray): BigInteger {
        var value = ZERO
        for (index in bits.lastIndex downTo 0) {
            value *= TWO
            if (bits[index]) value += ONE
        }
        return if (bits.lastOrNull() == true) value - TWO.pow(bits.size) else value
    }

    private fun toUnsignedByteArray(width: Int): ByteArray {
        var value = this
        val result = ByteArray(width)
        val base = of(256)
        for (index in result.lastIndex downTo 0) {
            val (quotient, remainder) = value.divideAndRemainder(base)
            result[index] = remainder.toIntExact().toByte()
            value = quotient
        }
        return result
    }

    companion object {
        val ZERO: BigInteger = BigInteger(0, intArrayOf(0))
        val ONE: BigInteger = BigInteger(1, intArrayOf(1))
        val TWO: BigInteger = BigInteger(1, intArrayOf(2))
        val THREE: BigInteger = BigInteger(1, intArrayOf(3))
        val TEN: BigInteger = BigInteger(1, intArrayOf(10))
        val MINUS_ONE: BigInteger = BigInteger(-1, intArrayOf(1))

        fun of(value: Int): BigInteger = of(value.toLong())

        fun of(value: Long): BigInteger {
            if (value == 0L) return ZERO
            val negative = value < 0L
            var remaining = if (negative) (-(value + 1L)).toULong() + 1uL else value.toULong()
            val digits = ArrayList<Int>(3)
            while (remaining != 0uL) {
                digits += (remaining % BASE.toULong()).toInt()
                remaining /= BASE.toULong()
            }
            return BigInteger(if (negative) -1 else 1, digits.toIntArray())
        }

        fun parse(value: String): BigInteger = parse(value, 10)

        /** Parses a signed value in a radix from 2 through 36. */
        fun parse(value: String, radix: Int): BigInteger {
            require(radix in 2..36) { "radix out of range: $radix" }
            require(value.isNotEmpty()) { "empty BigInteger" }
            var index = 0
            var sign = 1
            when (value[0]) {
                '-' -> {
                    sign = -1
                    index++
                }
                '+' -> index++
            }
            require(index < value.length) { "invalid BigInteger: $value" }
            var result = ZERO
            while (index < value.length) {
                val character = value[index++]
                val digit = character.digitValue(radix)
                require(digit >= 0) { "invalid BigInteger: $value" }
                result = result * radix + digit.toLong()
            }
            return if (sign < 0) -result else result
        }

        /** Parses Java `BigInteger(byte[])`'s signed two's-complement format. */
        fun fromByteArray(bytes: ByteArray): BigInteger {
            require(bytes.isNotEmpty()) { "zero-length BigInteger" }
            var value = ZERO
            for (byte in bytes) value = value * 256 + (byte.toInt() and 0xff).toLong()
            return if ((bytes.first().toInt() and 0x80) == 0) value else value - TWO.pow(bytes.size * 8)
        }

        /** Parses the explicitly signed magnitude format used by Java's `(signum, magnitude)` constructor. */
        fun fromSignMagnitude(signum: Int, magnitude: ByteArray): BigInteger {
            require(signum in -1..1) { "invalid signum: $signum" }
            var value = ZERO
            for (byte in magnitude) value = value * 256 + (byte.toInt() and 0xff).toLong()
            if (value.isZero) {
                require(signum == 0 || signum in -1..1) { "invalid signum" }
                return ZERO
            }
            require(signum != 0) { "signum-magnitude mismatch" }
            return if (signum < 0) -value else value
        }

        /** Portable counterpart to `new BigInteger(numBits, random)`. */
        fun random(bitLength: Int, random: Random = Random.Default): BigInteger {
            require(bitLength >= 0) { "negative bit length" }
            if (bitLength == 0) return ZERO
            val bytes = ByteArray((bitLength + 7) / 8) { random.nextInt(256).toByte() }
            val excessBits = bytes.size * 8 - bitLength
            bytes[0] = (bytes[0].toInt() and (0xff ushr excessBits)).toByte()
            return fromSignMagnitude(1, bytes)
        }

        /** Portable counterpart to `BigInteger.probablePrime(bitLength, random)`. */
        fun probablePrime(bitLength: Int, random: Random = Random.Default): BigInteger {
            require(bitLength >= 2) { "bitLength < 2" }
            while (true) {
                val candidate = random(bitLength, random).setBit(bitLength - 1).setBit(0)
                if (candidate.isProbablePrime(100)) return candidate
            }
        }

        private fun ofMagnitude(sign: Int, magnitude: IntArray): BigInteger {
            val normalized = normalize(magnitude)
            return if (normalized.size == 1 && normalized[0] == 0) ZERO else BigInteger(sign, normalized)
        }

        private fun normalize(input: IntArray): IntArray {
            var size = input.size
            while (size > 1 && input[size - 1] == 0) size--
            return if (size == input.size) input else input.copyOf(size)
        }

        private fun compareMagnitude(first: IntArray, second: IntArray): Int {
            if (first.size != second.size) return first.size.compareTo(second.size)
            for (index in first.lastIndex downTo 0) {
                if (first[index] != second[index]) return first[index].compareTo(second[index])
            }
            return 0
        }

        private fun addMagnitude(first: IntArray, second: IntArray): IntArray {
            val size = maxOf(first.size, second.size)
            val result = IntArray(size + 1)
            var carry = 0L
            for (index in 0 until size) {
                val sum = first.getOrElse(index) { 0 }.toLong() + second.getOrElse(index) { 0 }.toLong() + carry
                result[index] = (sum % BASE).toInt()
                carry = sum / BASE
            }
            result[size] = carry.toInt()
            return normalize(result)
        }

        /** Requires first >= second. */
        private fun subtractMagnitude(first: IntArray, second: IntArray): IntArray {
            val result = IntArray(first.size)
            var borrow = 0L
            for (index in first.indices) {
                var difference = first[index].toLong() - second.getOrElse(index) { 0 }.toLong() - borrow
                if (difference < 0L) {
                    difference += BASE
                    borrow = 1L
                } else {
                    borrow = 0L
                }
                result[index] = difference.toInt()
            }
            return normalize(result)
        }

        private fun multiplyMagnitude(first: IntArray, second: IntArray): IntArray {
            val result = LongArray(first.size + second.size + 1)
            for (i in first.indices) {
                var carry = 0L
                for (j in second.indices) {
                    val index = i + j
                    val product = result[index] + first[i].toLong() * second[j].toLong() + carry
                    result[index] = product % BASE
                    carry = product / BASE
                }
                var index = i + second.size
                while (carry != 0L) {
                    val sum = result[index] + carry
                    result[index] = sum % BASE
                    carry = sum / BASE
                    index++
                }
            }
            return normalize(IntArray(result.size) { result[it].toInt() })
        }

        private fun multiplyMagnitudeByLong(first: IntArray, multiplier: Long): IntArray {
            if (multiplier == 0L) return intArrayOf(0)
            val result = IntArray(first.size + 3)
            var carry = 0L
            for (index in first.indices) {
                val product = first[index].toLong() * multiplier + carry
                result[index] = (product % BASE).toInt()
                carry = product / BASE
            }
            var size = first.size
            while (carry != 0L) {
                result[size++] = (carry % BASE).toInt()
                carry /= BASE
            }
            return normalize(result.copyOf(size))
        }

        /** Magnitude-only long division; returns quotient then remainder. */
        private fun divideMagnitude(dividend: IntArray, divisor: IntArray): Pair<IntArray, IntArray> {
            val comparison = compareMagnitude(dividend, divisor)
            if (comparison < 0) return intArrayOf(0) to dividend.copyOf()
            if (comparison == 0) return intArrayOf(1) to intArrayOf(0)
            val quotient = IntArray(dividend.size)
            var remainder = intArrayOf(0)
            for (index in dividend.lastIndex downTo 0) {
                remainder = addMagnitude(multiplyMagnitudeByLong(remainder, BASE), intArrayOf(dividend[index]))
                var low = 0
                var high = BASE.toInt() - 1
                while (low <= high) {
                    val middle = low + (high - low) / 2
                    val candidate = multiplyMagnitudeByLong(divisor, middle.toLong())
                    if (compareMagnitude(candidate, remainder) <= 0) low = middle + 1 else high = middle - 1
                }
                quotient[index] = high
                if (high != 0) remainder = subtractMagnitude(remainder, multiplyMagnitudeByLong(divisor, high.toLong()))
            }
            return normalize(quotient) to normalize(remainder)
        }

        private const val BASE = 1_000_000_000L
        private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"
        private val SMALL_PRIMES = intArrayOf(
            3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59,
            61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131,
            137,
        )
        private val MIN_LONG_MAGNITUDE: BigInteger by lazy { of(Long.MIN_VALUE).abs() }
        private val LOG2_BASE = ln(BASE.toDouble()) / ln(2.0)
        private val LN_2 = ln(2.0)

        private fun Char.digitValue(radix: Int): Int {
            val value = when (this) {
                in '0'..'9' -> code - '0'.code
                in 'a'..'z' -> code - 'a'.code + 10
                in 'A'..'Z' -> code - 'A'.code + 10
                else -> -1
            }
            return if (value in 0 until radix) value else -1
        }
    }

    private fun checkedBit(bit: Int): Int {
        require(bit >= 0) { "negative bit address" }
        if (bit > Int.MAX_VALUE - 2) throw ArithmeticException("bit address out of range")
        return bit
    }
}
