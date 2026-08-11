package dev.guavakt.parity

import com.google.common.math.LongMath as GuavaLongMath
import dev.guavakt.math.LongMath
import dev.guavakt.math.RoundingMode
import java.math.RoundingMode as JavaRoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class LongMathDifferentialTest {
    @Test
    fun checkedAndSaturatedArithmeticMatchesGuavaAtLongBoundaries() {
        val values = listOf(
            Long.MIN_VALUE,
            Long.MIN_VALUE + 1,
            -4_294_967_296L,
            -2, -1, 0, 1, 2,
            4_294_967_296L,
            Long.MAX_VALUE - 1,
            Long.MAX_VALUE,
        )
        for (left in values) {
            for (right in values) {
                val label = "left=$left right=$right"
                assertEquals(outcome { GuavaLongMath.checkedAdd(left, right) }, outcome { LongMath.checkedAdd(left, right) }, "add $label")
                assertEquals(outcome { GuavaLongMath.checkedSubtract(left, right) }, outcome { LongMath.checkedSubtract(left, right) }, "subtract $label")
                assertEquals(outcome { GuavaLongMath.checkedMultiply(left, right) }, outcome { LongMath.checkedMultiply(left, right) }, "multiply $label")
                assertEquals(GuavaLongMath.saturatedAdd(left, right), LongMath.saturatedAdd(left, right), "saturated add $label")
                assertEquals(GuavaLongMath.saturatedSubtract(left, right), LongMath.saturatedSubtract(left, right), "saturated subtract $label")
                assertEquals(GuavaLongMath.saturatedMultiply(left, right), LongMath.saturatedMultiply(left, right), "saturated multiply $label")
            }
        }
    }

    @Test
    fun modulusGcdAndSaturatedAbsMatchGuavaForSignsAndExtremes() {
        val dividends = listOf(Long.MIN_VALUE, Long.MIN_VALUE + 1, -11, -1, 0, 1, 11, Long.MAX_VALUE)
        val moduli = listOf(1L, 2L, 3L, 7L, Int.MAX_VALUE.toLong(), Long.MAX_VALUE)
        for (dividend in dividends) {
            for (modulus in moduli) {
                assertEquals(GuavaLongMath.mod(dividend, modulus), LongMath.mod(dividend, modulus), "$dividend mod $modulus")
            }
        }
        val nonNegative = listOf(0L, 1L, 2L, 3L, 4L, 12L, Long.MAX_VALUE)
        for (left in nonNegative) {
            for (right in nonNegative) {
                assertEquals(GuavaLongMath.gcd(left, right), LongMath.gcd(left, right), "gcd($left, $right)")
            }
        }
        assertEquals(GuavaLongMath.saturatedAbs(Long.MIN_VALUE), LongMath.saturatedAbs(Long.MIN_VALUE))
    }

    @Test
    fun powerRoundingCombinatoricsAndMeanMatchGuava() {
        val powers = listOf(Long.MIN_VALUE, -3L, -2L, -1L, 0L, 1L, 2L, 3L, Long.MAX_VALUE)
        for (base in powers) {
            for (exponent in listOf(0, 1, 2, 3, 5, 62, 63, 64)) {
                assertEquals(GuavaLongMath.pow(base, exponent), LongMath.power(base, exponent), "pow($base, $exponent)")
                assertEquals(
                    outcome { GuavaLongMath.checkedPow(base, exponent) },
                    outcome { LongMath.checkedPower(base, exponent) },
                    "checkedPow($base, $exponent)",
                )
                assertEquals(
                    GuavaLongMath.saturatedPow(base, exponent),
                    LongMath.saturatedPower(base, exponent),
                    "saturatedPow($base, $exponent)",
                )
            }
        }

        val values = listOf(1L, 2L, 3L, 4L, 8L, 9L, 15L, 16L, 17L, 1L shl 62, (1L shl 62) + 1L, Long.MAX_VALUE)
        for (value in values) {
            assertEquals(outcome { GuavaLongMath.ceilingPowerOfTwo(value) }, outcome { LongMath.ceilingPowerOfTwo(value) })
            assertEquals(GuavaLongMath.floorPowerOfTwo(value), LongMath.floorPowerOfTwo(value))
        }
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for (value in listOf(0L, 1L, 2L, 3L, 4L, 8L, 9L, 15L, 16L, 17L, 3_037_000_499L * 3_037_000_499L, Long.MAX_VALUE)) {
                assertEquals(
                    outcome { GuavaLongMath.sqrt(value, javaMode) },
                    outcome { LongMath.sqrt(value, mode) },
                    "sqrt($value, $mode)",
                )
                if (value > 0L) {
                    assertEquals(
                        outcome { GuavaLongMath.log2(value, javaMode) },
                        outcome { LongMath.log2(value, mode) },
                        "log2($value, $mode)",
                    )
                    assertEquals(
                        outcome { GuavaLongMath.log10(value, javaMode) },
                        outcome { LongMath.log10(value, mode) },
                        "log10($value, $mode)",
                    )
                }
            }
            for ((p, q) in listOf(-17L to 5L, -17L to -5L, 17L to -5L, Long.MIN_VALUE to 3L, Long.MIN_VALUE to -1L, 7L to 0L)) {
                assertEquals(
                    outcome { GuavaLongMath.divide(p, q, javaMode) },
                    outcome { LongMath.divide(p, q, mode) },
                    "divide($p, $q, $mode)",
                )
            }
        }
        for (n in 0..25) assertEquals(GuavaLongMath.factorial(n), LongMath.factorial(n), "factorial($n)")
        for ((n, k) in listOf(0 to 0, 1 to 1, 50 to 3, 62 to 2, 66 to 2, 67 to 33)) {
            assertEquals(GuavaLongMath.binomial(n, k), LongMath.binomial(n, k), "binomial($n, $k)")
        }
        for ((x, y) in listOf(Long.MIN_VALUE to Long.MAX_VALUE, -3L to 4L, 0L to 1L, Long.MAX_VALUE to Long.MAX_VALUE)) {
            assertEquals(GuavaLongMath.mean(x, y), LongMath.mean(x, y), "mean($x, $y)")
        }
    }

    @Test
    fun deterministicPrimalityMatchesGuavaForBoundaryPseudoprimeAndSeededValues() {
        val values = mutableListOf(
            Long.MIN_VALUE, -1L, 0L, 1L, 2L, 3L, 4L, 5L, 9L, 97L,
            341_550_071_728_321L,
            382_512_305_654_641_305L,
            Long.MAX_VALUE,
        )
        val random = java.util.Random(0x5EED)
        repeat(128) { values += random.nextLong() and Long.MAX_VALUE }
        for (value in values) {
            assertEquals(
                outcome { GuavaLongMath.isPrime(value) },
                outcome { LongMath.isPrime(value) },
                "isPrime($value)",
            )
        }
    }

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
