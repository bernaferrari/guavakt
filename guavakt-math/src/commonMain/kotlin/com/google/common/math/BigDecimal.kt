package dev.guavakt.math

/**
 * Immutable arbitrary-precision decimal represented as `unscaledValue × 10^-scale`.
 *
 * Scale is retained, so `BigDecimal.parse("1.0")` and `BigDecimal.parse("1.00")` compare equal
 * numerically while preserving their distinct decimal presentations.
 */
class BigDecimal private constructor(
    val unscaledValue: BigInteger,
    val scale: Int,
) : Comparable<BigDecimal> {
    val signum: Int get() = unscaledValue.signum

    operator fun unaryMinus(): BigDecimal = if (unscaledValue.isZero) this else of(-unscaledValue, scale)

    /** Java-shaped alias for unary `-`. */
    fun negate(): BigDecimal = -this

    fun negate(context: MathContext): BigDecimal = negate().round(context)

    /** Java-shaped function form, alongside Kotlin's [signum] property. */
    fun signum(): Int = signum

    /** Java-shaped identity operation. */
    fun plus(): BigDecimal = this

    fun plus(context: MathContext): BigDecimal = round(context)

    fun abs(): BigDecimal = if (signum >= 0) this else -this

    fun abs(context: MathContext): BigDecimal = abs().round(context)

    fun scale(): Int = scale

    fun unscaledValue(): BigInteger = unscaledValue

    operator fun plus(other: BigDecimal): BigDecimal {
        val targetScale = maxOf(scale, other.scale)
        return of(
            scaleUnscaled(unscaledValue, (targetScale.toLong() - scale).toIntExact()) +
                scaleUnscaled(other.unscaledValue, (targetScale.toLong() - other.scale).toIntExact()),
            targetScale,
        )
    }

    /** Java-shaped alias for Kotlin's `+`. */
    fun add(other: BigDecimal): BigDecimal = this + other

    fun add(other: BigDecimal, context: MathContext): BigDecimal = (this + other).round(context)

    operator fun minus(other: BigDecimal): BigDecimal = this + -other

    /** Java-shaped alias for Kotlin's `-`. */
    fun subtract(other: BigDecimal): BigDecimal = this - other

    fun subtract(other: BigDecimal, context: MathContext): BigDecimal = (this - other).round(context)

    operator fun times(other: BigDecimal): BigDecimal =
        of(unscaledValue * other.unscaledValue, checkedScale(scale.toLong() + other.scale))

    /** Java-shaped alias for Kotlin's `*`. */
    fun multiply(other: BigDecimal): BigDecimal = this * other

    fun multiply(other: BigDecimal, context: MathContext): BigDecimal = (this * other).round(context)

    /**
     * Exact division with Java BigDecimal's preferred scale. Non-terminating decimal expansions
     * fail rather than silently selecting a precision.
     */
    operator fun div(other: BigDecimal): BigDecimal {
        if (other.unscaledValue.isZero) throw ArithmeticException("division by zero")
        val gcd = unscaledValue.gcd(other.unscaledValue)
        var numerator = unscaledValue / gcd
        var denominator = other.unscaledValue / gcd
        if (denominator.signum < 0) {
            numerator = -numerator
            denominator = -denominator
        }
        var twos = 0
        var fives = 0
        while ((denominator % BigInteger.TWO).isZero) {
            denominator /= BigInteger.TWO
            twos++
        }
        while ((denominator % BigInteger.of(5)).isZero) {
            denominator /= BigInteger.of(5)
            fives++
        }
        if (denominator.abs() != BigInteger.ONE) {
            throw ArithmeticException("non-terminating decimal expansion")
        }
        val decimalPlaces = maxOf(twos, fives)
        if (twos < decimalPlaces) numerator *= BigInteger.TWO.pow(decimalPlaces - twos)
        if (fives < decimalPlaces) numerator *= BigInteger.of(5).pow(decimalPlaces - fives)
        return of(numerator, checkedScale(scale.toLong() - other.scale + decimalPlaces))
    }

    /** Named exact-division form for Java/Guava migration call sites. */
    fun divide(other: BigDecimal): BigDecimal = this / other

    /**
     * Java's `divide(divisor, roundingMode)` form: retain this value's scale and round there.
     */
    fun divide(other: BigDecimal, mode: RoundingMode): BigDecimal = divide(other, scale, mode)

    /** Divides with an explicitly requested output [scale] and a Guava-shaped rounding mode. */
    fun divide(other: BigDecimal, scale: Int, mode: RoundingMode): BigDecimal {
        if (other.unscaledValue.isZero) throw ArithmeticException("division by zero")
        val exponent = scale.toLong() + other.scale - this.scale
        val numerator: BigInteger
        val denominator: BigInteger
        if (exponent >= 0) {
            numerator = unscaledValue * BigInteger.TEN.pow(exponent.toIntExact())
            denominator = other.unscaledValue
        } else {
            numerator = unscaledValue
            denominator = other.unscaledValue * BigInteger.TEN.pow((-exponent).toIntExact())
        }
        return of(BigIntegerMath.div(numerator, denominator, mode), scale)
    }

    /** Precision-context division with two guard digits before the final context rounding. */
    fun divide(other: BigDecimal, context: MathContext): BigDecimal {
        if (context.precision == 0) return this / other
        if (other.unscaledValue.isZero) throw ArithmeticException("division by zero")
        if (unscaledValue.isZero) return of(BigInteger.ZERO, checkedScale(scale.toLong() - other.scale)).round(context)
        val estimatedAdjusted = adjustedExponent() - other.adjustedExponent()
        val workingScale = checkedScale(context.precision.toLong() + 1L - estimatedAdjusted)
        return divide(other, workingScale, context.roundingMode).round(context)
    }

    /** Integer quotient with Java's preferred non-negative quotient scale. */
    fun divideToIntegralValue(other: BigDecimal): BigDecimal {
        if (other.unscaledValue.isZero) throw ArithmeticException("division by zero")
        val preferredScale = maxOf(0L, scale.toLong() - other.scale).toIntExactScale()
        val exponent = other.scale.toLong() - scale
        val quotient = if (exponent >= 0L) {
            (unscaledValue * BigInteger.TEN.pow(exponent.toIntExact())) / other.unscaledValue
        } else {
            unscaledValue / (other.unscaledValue * BigInteger.TEN.pow((-exponent).toIntExact()))
        }
        return of(quotient * BigInteger.TEN.pow(preferredScale), preferredScale)
    }

    fun divideToIntegralValue(other: BigDecimal, context: MathContext): BigDecimal =
        divideToIntegralValue(other).round(context)

    fun remainder(other: BigDecimal): BigDecimal = this - divideToIntegralValue(other) * other

    fun remainder(other: BigDecimal, context: MathContext): BigDecimal =
        (this - divideToIntegralValue(other, context) * other).round(context)

    fun divideAndRemainder(other: BigDecimal): Pair<BigDecimal, BigDecimal> {
        val quotient = divideToIntegralValue(other)
        return quotient to (this - quotient * other)
    }

    fun divideAndRemainder(other: BigDecimal, context: MathContext): Pair<BigDecimal, BigDecimal> {
        val quotient = divideToIntegralValue(other, context)
        return quotient to (this - quotient * other).round(context)
    }

    fun pow(exponent: Int): BigDecimal {
        require(exponent >= 0) { "Invalid operation" }
        return of(unscaledValue.pow(exponent), checkedScale(scale.toLong() * exponent))
    }

    fun pow(exponent: Int, context: MathContext): BigDecimal = pow(exponent).round(context)

    /**
     * Square root rounded under [context]. Unlimited precision accepts only an exactly finite
     * decimal root, matching Java's `MathContext.UNLIMITED` behavior.
     */
    fun sqrt(context: MathContext): BigDecimal {
        require(signum >= 0) { "Attempted square root of negative BigDecimal" }
        if (unscaledValue.isZero) return of(BigInteger.ZERO, scale / 2)
        if (context.precision == 0) {
            val canonical = stripTrailingZeros()
            if (canonical.scale % 2 != 0) throw ArithmeticException("Computed square root not exact")
            val root = BigIntegerMath.sqrt(canonical.unscaledValue, RoundingMode.FLOOR)
            if (root * root != canonical.unscaledValue) throw ArithmeticException("Computed square root not exact")
            return of(root, canonical.scale / 2)
        }

        val rootAdjusted = floorDiv(adjustedExponent(), 2L)
        val requestedScale = context.precision.toLong() + 1L - rootAdjusted
        val minimumScale = -floorDiv(-scale.toLong(), 2L)
        val workingScale = maxOf(requestedScale, minimumScale).toIntExactScale()
        val integerExponent = 2L * workingScale - scale.toLong()
        val radicand = unscaledValue * BigInteger.TEN.pow(integerExponent.toIntExact())
        val floor = BigIntegerMath.sqrt(radicand, RoundingMode.FLOOR)
        val rounded = if (floor * floor == radicand) {
            floor
        } else {
            when (context.roundingMode) {
                RoundingMode.UNNECESSARY -> throw ArithmeticException("Rounding necessary")
                RoundingMode.DOWN, RoundingMode.FLOOR -> floor
                RoundingMode.UP, RoundingMode.CEILING -> floor + BigInteger.ONE
                RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN -> {
                    val comparison = (radicand * BigInteger.of(4)).compareTo((floor * BigInteger.TWO + BigInteger.ONE).pow(2))
                    when {
                        comparison < 0 -> floor
                        comparison > 0 -> floor + BigInteger.ONE
                        context.roundingMode == RoundingMode.HALF_UP -> floor + BigInteger.ONE
                        context.roundingMode == RoundingMode.HALF_DOWN -> floor
                        else -> if ((floor % BigInteger.TWO).isZero) floor else floor + BigInteger.ONE
                    }
                }
            }
        }
        val result = of(rounded, workingScale).round(context)
        // Working guard digits must not leak into the representation. Java retains trailing zeroes
        // only down to sqrt's preferred scale (half of the source scale).
        return result.stripTrailingZerosToMinimumScale(scale / 2)
    }

    /** Changes decimal scale exactly or rounds the discarded fraction according to [mode]. */
    fun setScale(newScale: Int, mode: RoundingMode = RoundingMode.UNNECESSARY): BigDecimal =
        if (newScale >= scale) {
            of(unscaledValue * BigInteger.TEN.pow((newScale.toLong() - scale).toIntExact()), newScale)
        } else {
            of(
                BigIntegerMath.div(unscaledValue, BigInteger.TEN.pow((scale.toLong() - newScale).toIntExact()), mode),
                newScale,
            )
        }

    /** Moves the decimal point without introducing an exponent-specific representation. */
    fun movePointLeft(n: Int): BigDecimal {
        if (n == 0) return this
        val shifted = scaleByPowerOfTen(-n.toLong())
        return if (shifted.scale < 0) shifted.setScale(0) else shifted
    }

    fun movePointRight(n: Int): BigDecimal {
        if (n == 0) return this
        val shifted = scaleByPowerOfTen(n.toLong())
        return if (shifted.scale < 0) shifted.setScale(0) else shifted
    }

    /** Retains the unscaled coefficient while changing its power-of-ten scale. */
    fun scaleByPowerOfTen(n: Int): BigDecimal = scaleByPowerOfTen(n.toLong())

    private fun scaleByPowerOfTen(n: Long): BigDecimal = of(unscaledValue, checkedScale(scale.toLong() - n))

    /** Rounds to significant digits under a portable [MathContext]. */
    fun round(context: MathContext): BigDecimal {
        if (context.precision == 0 || precision() <= context.precision) return this
        val discardedDigits = precision() - context.precision
        var result = setScale(checkedScale(scale.toLong() - discardedDigits), context.roundingMode)
        // A carry may increase the coefficient's precision (999 -> 1000). Java moves an exact
        // trailing zero into the scale so that the requested significant-digit precision holds.
        while (result.precision() > context.precision) {
            result = of(result.unscaledValue / BigInteger.TEN, checkedScale(result.scale.toLong() - 1L))
        }
        return result
    }

    /** Removes base-ten trailing zeroes while retaining zero's canonical scale of zero. */
    fun stripTrailingZeros(): BigDecimal {
        if (unscaledValue.isZero) return ZERO
        var value = unscaledValue
        var resultScale = scale
        while ((value % BigInteger.TEN).isZero) {
            value /= BigInteger.TEN
            resultScale = checkedScale(resultScale.toLong() - 1)
        }
        return of(value, resultScale)
    }

    private fun stripTrailingZerosToMinimumScale(minimumScale: Int): BigDecimal {
        if (unscaledValue.isZero) return this
        var value = unscaledValue
        var resultScale = scale
        while (resultScale > minimumScale && (value % BigInteger.TEN).isZero) {
            value /= BigInteger.TEN
            resultScale--
        }
        return if (value == unscaledValue) this else of(value, resultScale)
    }

    fun precision(): Int = if (unscaledValue.isZero) 1 else unscaledValue.abs().toString().length

    fun min(other: BigDecimal): BigDecimal = if (this <= other) this else other

    fun max(other: BigDecimal): BigDecimal = if (this >= other) this else other

    /** Unit in the last place with this value's retained scale. */
    fun ulp(): BigDecimal = of(BigInteger.ONE, scale)

    /** Integer part with truncation toward zero, matching `java.math.BigDecimal.toBigInteger`. */
    fun toBigInteger(): BigInteger = when {
        scale == 0 -> unscaledValue
        scale > 0 -> unscaledValue / BigInteger.TEN.pow(scale)
        else -> unscaledValue * BigInteger.TEN.pow(checkedScale(-scale.toLong()))
    }

    fun toBigIntegerExact(): BigInteger {
        if (scale <= 0) return toBigInteger()
        val divisor = BigInteger.TEN.pow(scale)
        val (quotient, remainder) = unscaledValue.divideAndRemainder(divisor)
        if (!remainder.isZero) throw ArithmeticException("Rounding necessary")
        return quotient
    }

    fun toLong(): Long = toBigInteger().toLong()

    fun toInt(): Int = toBigInteger().toInt()

    fun toShort(): Short = toBigInteger().toShort()

    fun toByte(): Byte = toBigInteger().toByte()

    fun toLongExact(): Long = toBigIntegerExact().toLongExact()

    fun toIntExact(): Int = toBigIntegerExact().toIntExact()

    fun toShortExact(): Short = toBigIntegerExact().toShortExact()

    fun toByteExact(): Byte = toBigIntegerExact().toByteExact()

    fun toDouble(): Double = BigDecimalMath.roundToDouble(this, RoundingModeLike.HALF_EVEN)

    fun toFloat(): Float = "${unscaledValue}e${-scale}".toFloat()

    override fun compareTo(other: BigDecimal): Int {
        if (unscaledValue.signum != other.unscaledValue.signum) return unscaledValue.signum.compareTo(other.unscaledValue.signum)
        if (scale == other.scale) return unscaledValue.compareTo(other.unscaledValue)
        val comparison =
            if (scale < other.scale) (unscaledValue * BigInteger.TEN.pow(other.scale - scale)).compareTo(other.unscaledValue)
            else unscaledValue.compareTo(other.unscaledValue * BigInteger.TEN.pow(scale - other.scale))
        return comparison
    }

    override fun equals(other: Any?): Boolean =
        other is BigDecimal && unscaledValue == other.unscaledValue && scale == other.scale

    override fun hashCode(): Int = 31 * unscaledValue.hashCode() + scale

    /** Decimal form without an exponent, matching `java.math.BigDecimal.toPlainString`. */
    fun toPlainString(): String {
        if (scale == 0) return unscaledValue.toString()
        val negative = unscaledValue.signum < 0
        val digits = unscaledValue.abs().toString()
        return buildString(digits.length + 3 + maxOf(scale, 0)) {
            if (negative) append('-')
            when {
                scale < 0 -> {
                    append(digits)
                    repeat(-scale) { append('0') }
                }
                digits.length > scale -> {
                    append(digits.substring(0, digits.length - scale))
                    append('.')
                    append(digits.substring(digits.length - scale))
                }
                else -> {
                    append("0.")
                    repeat(scale - digits.length) { append('0') }
                    append(digits)
                }
            }
        }
    }

    /** Scientific representation whose exponent is a multiple of three. */
    fun toEngineeringString(): String {
        val digits = unscaledValue.abs().toString()
        val adjusted = digits.length.toLong() - scale - 1L
        if (scale >= 0 && adjusted >= -6) return toPlainString()
        val exponent = adjusted - floorMod(adjusted, 3L)
        val integerDigits = (adjusted - exponent + 1L).toInt()
        return buildString(digits.length + 10) {
            if (unscaledValue.signum < 0) append('-')
            when {
                digits.length <= integerDigits -> {
                    append(digits)
                    repeat(integerDigits - digits.length) { append('0') }
                }
                else -> {
                    append(digits.substring(0, integerDigits))
                    append('.')
                    append(digits.substring(integerDigits))
                }
            }
            if (exponent != 0L) {
                append('E')
                if (exponent > 0) append('+')
                append(exponent)
            }
        }
    }

    override fun toString(): String {
        val digits = unscaledValue.abs().toString()
        val adjusted = digits.length.toLong() - scale - 1L
        // This is Java BigDecimal's plain/scientific boundary. Scale is deliberately retained,
        // so zero and values with trailing decimal zeroes continue to render distinctly.
        if (scale >= 0 && adjusted >= -6) return toPlainString()
        return buildString(digits.length + 8) {
            if (unscaledValue.signum < 0) append('-')
            append(digits[0])
            if (digits.length > 1) {
                append('.')
                append(digits.substring(1))
            }
            append('E')
            if (adjusted >= 0) append('+')
            append(adjusted)
        }
    }

    companion object {
        val ZERO: BigDecimal = BigDecimal(BigInteger.ZERO, 0)
        val ONE: BigDecimal = BigDecimal(BigInteger.ONE, 0)
        val TEN: BigDecimal = BigDecimal(BigInteger.TEN, 0)

        fun of(unscaledValue: BigInteger, scale: Int = 0): BigDecimal = BigDecimal(unscaledValue, scale)

        fun of(value: Long): BigDecimal = of(BigInteger.of(value))

        /** Portable counterpart to `BigDecimal.valueOf(double)`. */
        fun of(value: Double): BigDecimal {
            require(value.isFinite()) { "not a finite double: $value" }
            return parse(value.toString())
        }

        /** Exact decimal expansion of a finite IEEE-754 `Double`, like `new BigDecimal(double)`. */
        fun fromDoubleExact(value: Double): BigDecimal {
            require(value.isFinite()) { "not a finite double: $value" }
            if (value == 0.0) return ZERO
            val bits = value.toRawBits()
            val negative = bits < 0L
            val exponentBits = ((bits ushr 52) and 0x7ffL).toInt()
            val fraction = bits and 0x000fffffffffffffL
            var significand = if (exponentBits == 0) fraction else fraction or 0x0010000000000000L
            if (negative) significand = -significand
            val binaryExponent = if (exponentBits == 0) -1074 else exponentBits - 1075
            return if (binaryExponent >= 0) {
                of(BigInteger.of(significand).shiftLeft(binaryExponent), 0)
            } else {
                val decimalScale = -binaryExponent
                // A binary significand can carry powers of two.  Once converted to a
                // `5^scale` numerator they become decimal trailing zeroes; Java's
                // `BigDecimal(double)` removes those representation-only zeroes.
                of(BigInteger.of(significand) * BigInteger.of(5).pow(decimalScale), decimalScale)
                    .stripTrailingZeros()
            }
        }

        private fun checkedScale(value: Long): Int {
            if (value !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                throw ArithmeticException("BigDecimal scale out of Int range")
            }
            return value.toInt()
        }

        fun parse(source: String): BigDecimal {
            require(source.isNotEmpty()) { "empty BigDecimal" }
            var index = 0
            var negative = false
            if (source[index] == '+' || source[index] == '-') {
                negative = source[index] == '-'
                index++
            }
            require(index < source.length) { "invalid BigDecimal: $source" }
            val digits = StringBuilder(source.length)
            var fractionalDigits = 0
            var dotSeen = false
            while (index < source.length && source[index] != 'e' && source[index] != 'E') {
                val character = source[index++]
                if (character == '.') {
                    require(!dotSeen) { "invalid BigDecimal: $source" }
                    dotSeen = true
                } else {
                    require(character in '0'..'9') { "invalid BigDecimal: $source" }
                    digits.append(character)
                    if (dotSeen) fractionalDigits++
                }
            }
            require(digits.isNotEmpty()) { "invalid BigDecimal: $source" }
            var exponent = 0
            if (index < source.length) {
                index++
                require(index < source.length) { "invalid BigDecimal: $source" }
                val exponentStart = index
                if (source[index] == '+' || source[index] == '-') index++
                require(index < source.length) { "invalid BigDecimal: $source" }
                while (index < source.length) {
                    require(source[index] in '0'..'9') { "invalid BigDecimal: $source" }
                    index++
                }
                exponent = source.substring(exponentStart).toIntOrNull()
                    ?: throw IllegalArgumentException("BigDecimal exponent out of Int range: $source")
            }
            var integer = BigInteger.parse(digits.toString())
            if (negative) integer = -integer
            return of(integer, checkedScale(fractionalDigits.toLong() - exponent))
        }
    }

    private fun scaleUnscaled(value: BigInteger, delta: Int): BigInteger =
        if (delta == 0) value else value * BigInteger.TEN.pow(delta)

    /** Decimal exponent of the most significant digit, with zero treated as exponent zero. */
    private fun adjustedExponent(): Long =
        if (unscaledValue.isZero) 0L else precision().toLong() - scale.toLong() - 1L

    private fun Long.toIntExact(): Int {
        if (this !in 0L..Int.MAX_VALUE.toLong()) throw ArithmeticException("BigDecimal scale difference out of Int range")
        return toInt()
    }

    private fun Long.toIntExactScale(): Int = checkedScale(this)

    private fun floorMod(value: Long, divisor: Long): Long {
        val remainder = value % divisor
        return if (remainder < 0L) remainder + divisor else remainder
    }

    private fun floorDiv(value: Long, divisor: Long): Long {
        val quotient = value / divisor
        val remainder = value % divisor
        return if (remainder != 0L && (value xor divisor) < 0L) quotient - 1L else quotient
    }
}
