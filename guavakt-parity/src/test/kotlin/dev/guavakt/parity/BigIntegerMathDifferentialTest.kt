package dev.guavakt.parity

import com.google.common.math.BigIntegerMath as GuavaBigIntegerMath
import dev.guavakt.math.BigInteger as GuavaKtBigInteger
import dev.guavakt.math.BigIntegerMath
import dev.guavakt.math.RoundingMode
import java.math.BigInteger
import java.math.RoundingMode as JavaRoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.random.Random

class BigIntegerMathDifferentialTest {
    @Test
    fun bigIntegerSqrtAndLog2MatchGuavaForEveryRoundingMode() {
        val nearMaxSquare = 3_037_000_499L * 3_037_000_499L
        val values = listOf(0L, 1L, 2L, 3L, 4L, 8L, 9L, 15L, 16L, 17L, nearMaxSquare, Long.MAX_VALUE)
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for (value in values) {
                assertEquals(
                    outcome { GuavaBigIntegerMath.sqrt(BigInteger.valueOf(value), javaMode).toString() },
                    outcome { BigIntegerMath.sqrt(GuavaKtBigInteger.of(value), mode).toString() },
                    "sqrt value=$value mode=$mode",
                )
                if (value > 0L) {
                    assertEquals(
                        outcome { GuavaBigIntegerMath.log2(BigInteger.valueOf(value), javaMode) },
                        outcome { BigIntegerMath.log2(value, mode) },
                        "log2 value=$value mode=$mode",
                    )
                }
            }
        }
    }

    @Test
    fun bigIntegerDivisionFactorialAndBinomialMatchGuavaBeyondLongRange() {
        val divisions = listOf(
            -17L to 5L,
            -17L to -5L,
            17L to -5L,
            Long.MIN_VALUE to 3L,
            Long.MAX_VALUE to Long.MIN_VALUE,
            7L to 0L,
        )
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for ((p, q) in divisions) {
                assertEquals(
                    outcome { GuavaBigIntegerMath.divide(BigInteger.valueOf(p), BigInteger.valueOf(q), javaMode).toString() },
                    outcome { BigIntegerMath.div(GuavaKtBigInteger.of(p), GuavaKtBigInteger.of(q), mode).toString() },
                    "divide p=$p q=$q mode=$mode",
                )
            }
        }
        for (n in listOf(0, 1, 2, 20, 50, 100)) {
            assertEquals(
                GuavaBigIntegerMath.factorial(n).toString(),
                BigIntegerMath.factorial(n).toString(),
                "factorial n=$n",
            )
        }
        for ((n, k) in listOf(0 to 0, 1 to 0, 1 to 1, 50 to 3, 62 to 2, 66 to 2, 200 to 100)) {
            assertEquals(
                GuavaBigIntegerMath.binomial(n, k).toString(),
                BigIntegerMath.binomial(n, k).toString(),
                "binomial n=$n k=$k",
            )
        }
    }

    @Test
    fun parsedHundredDigitValuesMatchGuava() {
        val source = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
        val guava = BigInteger(source)
        val kotlin = GuavaKtBigInteger.parse(source)
        assertEquals(guava.multiply(guava).toString(), (kotlin * kotlin).toString())
        assertEquals(GuavaBigIntegerMath.sqrt(guava.multiply(guava), JavaRoundingMode.UNNECESSARY).toString(), BigIntegerMath.sqrt(kotlin * kotlin, RoundingMode.UNNECESSARY).toString())
    }

    /**
     * Deterministic cross-runtime fuzzing over values far beyond JVM primitive ranges.
     *
     * The default is deliberately substantial enough to cover limb carries, signs, quotient
     * estimates, and rounding ties in the regular test suite. `-PfuzzSeeds` and
     * `-PfuzzCases` can raise it for an overnight direct-Guava run.
     */
    @Test
    fun seededArbitraryPrecisionArithmeticFuzzMatchesJdkAndGuava() {
        val seeds = System.getProperty("guavakt.fuzz.seeds", "12").toInt()
        val casesPerSeed = System.getProperty("guavakt.fuzz.cases", "96").toInt()
        repeat(seeds) { seed ->
            val random = Random(seed)
            repeat(casesPerSeed) { case ->
                val leftSource = randomDecimal(random)
                val rightSource = randomDecimal(random)
                val leftJava = BigInteger(leftSource)
                val rightJava = BigInteger(rightSource)
                val left = GuavaKtBigInteger.parse(leftSource)
                val right = GuavaKtBigInteger.parse(rightSource)
                val context = "seed=$seed case=$case left=$leftSource right=$rightSource"

                assertEquals(leftJava.add(rightJava).toString(), (left + right).toString(), "$context add")
                assertEquals(leftJava.subtract(rightJava).toString(), (left - right).toString(), "$context subtract")
                assertEquals(leftJava.multiply(rightJava).toString(), (left * right).toString(), "$context multiply")
                assertEquals(leftJava.compareTo(rightJava), left.compareTo(right), "$context compare")
                assertEquals(leftJava.abs().toString(), left.abs().toString(), "$context abs")

                if (rightJava.signum() != 0) {
                    assertEquals(leftJava.divide(rightJava).toString(), (left / right).toString(), "$context divide")
                    assertEquals(leftJava.remainder(rightJava).toString(), (left % right).toString(), "$context remainder")
                    val mode = RoundingMode.entries[random.nextInt(RoundingMode.entries.size)]
                    assertEquals(
                        outcome { GuavaBigIntegerMath.divide(leftJava, rightJava, JavaRoundingMode.valueOf(mode.name)).toString() },
                        outcome { BigIntegerMath.div(left, right, mode).toString() },
                        "$context rounded divide mode=$mode",
                    )
                }

                val positiveJava = leftJava.abs().add(BigInteger.ONE)
                val positive = left.abs() + GuavaKtBigInteger.ONE
                val mode = RoundingMode.entries[random.nextInt(RoundingMode.entries.size)]
                assertEquals(
                    outcome { GuavaBigIntegerMath.sqrt(positiveJava, JavaRoundingMode.valueOf(mode.name)).toString() },
                    outcome { BigIntegerMath.sqrt(positive, mode).toString() },
                    "$context sqrt mode=$mode",
                )
                assertEquals(
                    outcome { GuavaBigIntegerMath.log2(positiveJava, JavaRoundingMode.valueOf(mode.name)) },
                    outcome { BigIntegerMath.log2(positive, mode) },
                    "$context log2 mode=$mode",
                )
            }
        }
    }

    private fun randomDecimal(random: Random): String = buildString {
        if (random.nextInt(7) == 0) {
            append('0')
            return@buildString
        }
        if (random.nextBoolean()) append('-')
        append(random.nextInt(1, 10))
        repeat(random.nextInt(0, 180)) { append(random.nextInt(10)) }
    }

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
