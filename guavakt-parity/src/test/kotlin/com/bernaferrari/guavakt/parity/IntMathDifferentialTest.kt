package com.bernaferrari.guavakt.parity

import com.google.common.math.IntMath as GuavaIntMath
import com.bernaferrari.guavakt.math.IntMath
import com.bernaferrari.guavakt.math.RoundingMode
import java.math.RoundingMode as JavaRoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class IntMathDifferentialTest {
    @Test
    fun uncheckedPowerOverflowAndBoundaryCasesMatchGuava() {
        val bases = listOf(-100_000, -46_341, -46_340, -2, -1, 0, 1, 2, 46_340, 46_341, 100_000)
        val exponents = listOf(0, 1, 2, 3, 5, 30, 31, 32)

        for (base in bases) {
            for (exponent in exponents) {
                assertEquals(
                    GuavaIntMath.pow(base, exponent),
                    IntMath.power(base, exponent),
                    "base=$base, exponent=$exponent",
                )
                assertEquals(
                    outcome { GuavaIntMath.checkedPow(base, exponent) },
                    outcome { IntMath.checkedPower(base, exponent) },
                    "checked base=$base, exponent=$exponent",
                )
                assertEquals(
                    GuavaIntMath.saturatedPow(base, exponent),
                    IntMath.saturatedPower(base, exponent),
                    "saturated base=$base, exponent=$exponent",
                )
            }
        }
    }

    @Test
    fun roundedDivisionRootsLogsAndFactorialsMatchGuavaAtIntBoundaries() {
        val values = listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -17, -5, -1, 0, 1, 2, 3, 5, 17, Int.MAX_VALUE)
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for (p in values) {
                for (q in values) {
                    assertEquals(
                        outcome { GuavaIntMath.divide(p, q, javaMode) },
                        outcome { IntMath.divide(p, q, mode) },
                        "divide p=$p q=$q mode=$mode",
                    )
                }
            }
            for (x in listOf(0, 1, 2, 3, 4, 8, 9, 15, 16, 17, 2_147_395_600, Int.MAX_VALUE)) {
                assertEquals(
                    outcome { GuavaIntMath.sqrt(x, javaMode) },
                    outcome { IntMath.sqrt(x, mode) },
                    "sqrt x=$x mode=$mode",
                )
                if (x > 0) {
                    assertEquals(
                        outcome { GuavaIntMath.log2(x, javaMode) },
                        outcome { IntMath.log2(x, mode) },
                        "log2 x=$x mode=$mode",
                    )
                }
            }
        }
        for (n in 0..20) {
            assertEquals(GuavaIntMath.factorial(n), IntMath.factorial(n), "factorial n=$n")
        }
    }

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
