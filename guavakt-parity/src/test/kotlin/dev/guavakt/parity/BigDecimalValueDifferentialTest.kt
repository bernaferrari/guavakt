package dev.guavakt.parity

import dev.guavakt.math.BigDecimal as GuavaKtBigDecimal
import dev.guavakt.math.MathContext as GuavaKtMathContext
import dev.guavakt.math.RoundingMode as GuavaKtRoundingMode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalValueDifferentialTest {
    @Test
    fun presentationPointAndIntegralOperationsMatchTheJdk() {
        val sources = listOf("0.00", "0E+3", "123456789.00", "0.0000001", "-1200.00", "999.5")
        for (source in sources) {
            val java = BigDecimal(source)
            val kotlin = GuavaKtBigDecimal.parse(source)
            assertEquals(java.toEngineeringString(), kotlin.toEngineeringString(), "engineering $source")
            assertEquals(java.ulp().toString(), kotlin.ulp().toString(), "ulp $source")
            for (move in listOf(-6, -1, 0, 1, 6)) {
                assertEquals(java.movePointLeft(move).toString(), kotlin.movePointLeft(move).toString(), "left $source $move")
                assertEquals(java.movePointRight(move).toString(), kotlin.movePointRight(move).toString(), "right $source $move")
                assertEquals(java.scaleByPowerOfTen(move).toString(), kotlin.scaleByPowerOfTen(move).toString(), "power $source $move")
            }
            assertEquals(java.pow(3).toString(), kotlin.pow(3).toString(), "pow $source")
        }

        val pairs = listOf("123.45" to "0.1", "5.0" to "2.00", "-10.5" to "3", "100" to "7")
        for ((leftSource, rightSource) in pairs) {
            val javaLeft = BigDecimal(leftSource)
            val javaRight = BigDecimal(rightSource)
            val left = GuavaKtBigDecimal.parse(leftSource)
            val right = GuavaKtBigDecimal.parse(rightSource)
            assertEquals(javaLeft.divideToIntegralValue(javaRight).toString(), left.divideToIntegralValue(right).toString(), "integral $leftSource/$rightSource")
            assertEquals(javaLeft.remainder(javaRight).toString(), left.remainder(right).toString(), "remainder $leftSource/$rightSource")
            assertEquals(
                javaLeft.divideAndRemainder(javaRight).joinToString(",") { it.toString() },
                left.divideAndRemainder(right).let { "${it.first},${it.second}" },
                "pair $leftSource/$rightSource",
            )
        }
    }

    @Test
    fun mathContextsAndFloatingConstructionMatchTheJdk() {
        val contexts = listOf(MathContext(2, RoundingMode.HALF_UP), MathContext.DECIMAL32, MathContext.DECIMAL64)
        val leftSource = "12345.6789"
        val rightSource = "7.321"
        for (javaContext in contexts) {
            val kotlinContext = GuavaKtMathContext(javaContext.precision, GuavaKtRoundingMode.valueOf(javaContext.roundingMode.name))
            val javaLeft = BigDecimal(leftSource)
            val javaRight = BigDecimal(rightSource)
            val left = GuavaKtBigDecimal.parse(leftSource)
            val right = GuavaKtBigDecimal.parse(rightSource)
            assertEquals(javaLeft.round(javaContext).toString(), left.round(kotlinContext).toString(), "round $javaContext")
            assertEquals(javaLeft.add(javaRight, javaContext).toString(), left.add(right, kotlinContext).toString(), "add $javaContext")
            assertEquals(javaLeft.multiply(javaRight, javaContext).toString(), left.multiply(right, kotlinContext).toString(), "multiply $javaContext")
            assertEquals(javaLeft.divide(javaRight, javaContext).toString(), left.divide(right, kotlinContext).toString(), "divide $javaContext")
            for (source in listOf("2", "2.25", "0.0004", "123456789.987654321")) {
                assertEquals(
                    BigDecimal(source).sqrt(javaContext).toString(),
                    GuavaKtBigDecimal.parse(source).sqrt(kotlinContext).toString(),
                    "sqrt $source $javaContext",
                )
            }
        }

        for (value in listOf(0.1, -0.1, Math.PI, Double.MIN_VALUE, 1.0e100)) {
            assertEquals(BigDecimal(value).toString(), GuavaKtBigDecimal.fromDoubleExact(value).toString(), "exact $value")
            assertEquals(BigDecimal.valueOf(value).toString(), GuavaKtBigDecimal.of(value).toString(), "valueOf $value")
        }
    }
}
