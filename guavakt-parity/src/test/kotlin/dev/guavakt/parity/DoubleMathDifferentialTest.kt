package dev.guavakt.parity

import com.google.common.math.DoubleMath as GuavaDoubleMath
import dev.guavakt.math.DoubleMath
import dev.guavakt.math.RoundingMode
import java.math.RoundingMode as JavaRoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class DoubleMathDifferentialTest {
    @Test
    fun roundToLongMatchesGuavaAcrossModesTiesAndExtremeDoubles() {
        val values = listOf(
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            -Double.MAX_VALUE,
            Long.MIN_VALUE.toDouble(),
            Long.MIN_VALUE.toDouble() + 2_048.0,
            -2.6, -2.5, -2.4, -1.5, -0.5, -0.1, -0.0,
            0.0, 0.1, 0.5, 1.5, 2.4, 2.5, 2.6,
            Long.MAX_VALUE.toDouble() - 2_048.0,
            Long.MAX_VALUE.toDouble(),
            Double.MAX_VALUE,
        )
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for (value in values) {
                assertEquals(
                    outcome { GuavaDoubleMath.roundToLong(value, javaMode) },
                    outcome { DoubleMath.roundToLong(value, mode) },
                    "value=$value mode=$mode",
                )
            }
        }
    }

    @Test
    fun roundToIntAndIntegerRecognitionMatchGuavaAtRepresentationalBoundaries() {
        val values = listOf(
            Double.NaN,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Int.MIN_VALUE.toDouble() - 1.0,
            Int.MIN_VALUE.toDouble(),
            -1.5, -0.0, 0.0, 0.5, 1.5,
            Int.MAX_VALUE.toDouble(),
            Int.MAX_VALUE.toDouble() + 1.0,
            9_007_199_254_740_992.0,
            9_007_199_254_740_993.0,
        )
        for (mode in RoundingMode.entries) {
            val javaMode = JavaRoundingMode.valueOf(mode.name)
            for (value in values) {
                assertEquals(
                    outcome { GuavaDoubleMath.roundToInt(value, javaMode) },
                    outcome { DoubleMath.roundToInt(value, mode) },
                    "roundToInt value=$value mode=$mode",
                )
            }
        }
        assertEquals(
            values.map(GuavaDoubleMath::isMathematicalInteger),
            values.map(DoubleMath::isMathematicalInteger),
        )
    }

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
