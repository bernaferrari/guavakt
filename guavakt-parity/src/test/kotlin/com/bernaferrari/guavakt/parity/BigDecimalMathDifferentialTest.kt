package com.bernaferrari.guavakt.parity

import com.google.common.math.BigDecimalMath as GuavaBigDecimalMath
import com.bernaferrari.guavakt.math.BigDecimal as GuavaKtBigDecimal
import com.bernaferrari.guavakt.math.BigDecimalMath as GuavaKtBigDecimalMath
import com.bernaferrari.guavakt.math.RoundingMode as GuavaKtRoundingMode
import com.bernaferrari.guavakt.math.RoundingModeLike
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalMathDifferentialTest {
    @Test
    fun finiteLongDecimalTierMatchesGuavaForEveryRoundingMode() {
        val values = listOf(
            Long.MIN_VALUE,
            -9_007_199_254_740_993L,
            -10L,
            -1L,
            0L,
            1L,
            10L,
            9_007_199_254_740_993L,
            Long.MAX_VALUE,
        )
        val scales = listOf(-18, -6, -1, 0, 1, 6, 18)

        values.forEach { value ->
            scales.forEach { scale ->
                RoundingModeLike.entries.forEach { mode ->
                    assertEquals(
                        guavaOutcome(value, scale, mode),
                        kotlinOutcome(value, scale, mode),
                        "value=$value scale=$scale mode=$mode",
                    )
                }
            }
        }
    }

    @Test
    fun seededLongDecimalRoundingTraceMatchesGuava() {
        var state = 0x4d595df4d0f33173L
        repeat(512) { step ->
            state = state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            val value = state
            val scale = (((state ushr 17) % 37L).toInt() - 18)
            val mode = RoundingModeLike.entries[((state ushr 43) % RoundingModeLike.entries.size).toInt()]
            assertEquals(
                guavaOutcome(value, scale, mode),
                kotlinOutcome(value, scale, mode),
                "step=$step value=$value scale=$scale mode=$mode",
            )
        }
    }

    @Test
    fun arbitraryPrecisionDecimalStringsMatchGuavaRoundToDouble() {
        val values = listOf(
            "1234567890123456789012345678901234567890.12345678901234567890",
            "-9876543210987654321098765432109876543210.5",
            "1e100",
            "3e-324",
            "2e-324",
            "1e400",
            "-1e400",
        )
        for (source in values) {
            for (mode in RoundingModeLike.entries) {
                assertEquals(
                    bigOutcome { GuavaBigDecimalMath.roundToDouble(BigDecimal(source), RoundingMode.valueOf(mode.name)).toRawBits() },
                    bigOutcome { GuavaKtBigDecimalMath.roundToDouble(GuavaKtBigDecimal.parse(source), mode).toRawBits() },
                    "source=$source mode=$mode",
                )
            }
        }
    }

    @Test
    fun arbitraryPrecisionDecimalValuesMatchJdkArithmeticAndScaleContracts() {
        val pairs = listOf(
            "100.00" to "2.0",
            "-123456789012345678901234567890.50" to "7.25",
            "1E+100" to "0.000001",
            "0.0000001" to "-2",
        )
        for ((leftSource, rightSource) in pairs) {
            val javaLeft = BigDecimal(leftSource)
            val javaRight = BigDecimal(rightSource)
            val left = GuavaKtBigDecimal.parse(leftSource)
            val right = GuavaKtBigDecimal.parse(rightSource)
            val context = "left=$leftSource right=$rightSource"
            assertEquals(javaLeft.toString(), left.toString(), "$context parse/toString")
            assertEquals(javaLeft.toPlainString(), left.toPlainString(), "$context plain")
            assertEquals(javaLeft.add(javaRight).toString(), (left + right).toString(), "$context add")
            assertEquals(javaLeft.subtract(javaRight).toString(), (left - right).toString(), "$context subtract")
            assertEquals(javaLeft.multiply(javaRight).toString(), (left * right).toString(), "$context multiply")
            assertEquals(
                bigOutcome { javaLeft.divide(javaRight).toString() },
                bigOutcome { (left / right).toString() },
                "$context exact divide",
            )
            for (mode in GuavaKtRoundingMode.entries) {
                val javaMode = RoundingMode.valueOf(mode.name)
                assertEquals(
                    bigOutcome { javaLeft.divide(javaRight, 9, javaMode).toString() },
                    bigOutcome { left.divide(right, 9, mode).toString() },
                    "$context scaled divide mode=$mode",
                )
                assertEquals(
                    bigOutcome { javaLeft.setScale(1, javaMode).toString() },
                    bigOutcome { left.setScale(1, mode).toString() },
                    "$context setScale mode=$mode",
                )
            }
        }

        for (source in listOf("12300.00", "0.0000001", "0E+3", "-1200.00")) {
            val java = BigDecimal(source)
            val kotlin = GuavaKtBigDecimal.parse(source)
            assertEquals(java.stripTrailingZeros().toString(), kotlin.stripTrailingZeros().toString(), "strip source=$source")
            assertEquals(java.precision(), kotlin.precision(), "precision source=$source")
            assertEquals(java.toBigInteger().toString(), kotlin.toBigInteger().toString(), "integer source=$source")
            assertEquals(
                bigOutcome { java.toBigIntegerExact().toString() },
                bigOutcome { kotlin.toBigIntegerExact().toString() },
                "exact integer source=$source",
            )
        }
    }

    private fun guavaOutcome(value: Long, scale: Int, mode: RoundingModeLike): Outcome = try {
        Outcome(bits = GuavaBigDecimalMath.roundToDouble(BigDecimal.valueOf(value, scale), RoundingMode.valueOf(mode.name)).toRawBits())
    } catch (failure: Throwable) {
        Outcome(failure = failure::class.simpleName)
    }

    private fun kotlinOutcome(value: Long, scale: Int, mode: RoundingModeLike): Outcome = try {
        Outcome(bits = GuavaKtBigDecimalMath.roundToDouble(value, scale, mode).toRawBits())
    } catch (failure: Throwable) {
        Outcome(failure = failure::class.simpleName)
    }

    private fun <T> bigOutcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }

    private data class Outcome(val bits: Long? = null, val failure: String? = null)
}
