package dev.guavakt.parity

import com.google.common.math.LinearTransformation as GuavaLinearTransformation
import dev.guavakt.math.LinearTransformation
import kotlin.test.Test
import kotlin.test.assertEquals

class LinearTransformationDifferentialTest {
    @Test
    fun finiteRegularHorizontalVerticalAndNanTransformationsMatchGuava() {
        val guava = listOf(
            GuavaLinearTransformation.mapping(1.0, 2.0).and(3.0, 6.0),
            GuavaLinearTransformation.horizontal(-2.0),
            GuavaLinearTransformation.vertical(3.0),
            GuavaLinearTransformation.mapping(1.0, 2.0).withSlope(Double.POSITIVE_INFINITY),
            GuavaLinearTransformation.forNaN(),
        )
        val ours = listOf(
            LinearTransformation.mapping(1.0, 2.0).and(3.0, 6.0),
            LinearTransformation.horizontal(-2.0),
            LinearTransformation.vertical(3.0),
            LinearTransformation.mapping(1.0, 2.0).withSlope(Double.POSITIVE_INFINITY),
            LinearTransformation.forNaN(),
        )

        assertEquals(guava.map(::snapshot), ours.map(::snapshot))
    }

    @Test
    fun invalidCoordinatesAndSlopesMatchGuava() {
        val invalid = listOf(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        for (value in invalid) {
            assertEquals(
                outcome { GuavaLinearTransformation.mapping(value, 1.0) },
                outcome { LinearTransformation.mapping(value, 1.0) },
            )
            assertEquals(
                outcome { GuavaLinearTransformation.horizontal(value) },
                outcome { LinearTransformation.horizontal(value) },
            )
            assertEquals(
                outcome { GuavaLinearTransformation.vertical(value) },
                outcome { LinearTransformation.vertical(value) },
            )
            assertEquals(
                outcome { GuavaLinearTransformation.mapping(1.0, 1.0).and(2.0, value) },
                outcome { LinearTransformation.mapping(1.0, 1.0).and(2.0, value) },
            )
        }
        assertEquals(
            outcome { GuavaLinearTransformation.mapping(1.0, 1.0).and(1.0, 1.0) },
            outcome { LinearTransformation.mapping(1.0, 1.0).and(1.0, 1.0) },
        )
        assertEquals(
            outcome { GuavaLinearTransformation.mapping(1.0, 1.0).withSlope(Double.NaN) },
            outcome { LinearTransformation.mapping(1.0, 1.0).withSlope(Double.NaN) },
        )
    }

    private fun snapshot(transformation: GuavaLinearTransformation): List<Any?> = snapshot(
        transformation.isVertical(),
        transformation.isHorizontal(),
        { transformation.slope() },
        { value -> transformation.transform(value) },
        transformation.inverse().isVertical(),
        transformation.inverse().isHorizontal(),
        { transformation.inverse().slope() },
    )

    private fun snapshot(transformation: LinearTransformation): List<Any?> = snapshot(
        transformation.isVertical(),
        transformation.isHorizontal(),
        { transformation.slope() },
        { value -> transformation.transform(value) },
        transformation.inverse().isVertical(),
        transformation.inverse().isHorizontal(),
        { transformation.inverse().slope() },
    )

    private fun snapshot(
        vertical: Boolean,
        horizontal: Boolean,
        slope: () -> Double,
        transform: (Double) -> Double,
        inverseVertical: Boolean,
        inverseHorizontal: Boolean,
        inverseSlope: () -> Double,
    ): List<Any?> = listOf(
        vertical,
        horizontal,
        outcomeDouble(slope),
        listOf(-2.0, -0.0, 0.0, 1.5, Double.NaN).map { outcomeDouble { transform(it) } },
        inverseVertical,
        inverseHorizontal,
        outcomeDouble(inverseSlope),
    )

    private fun outcome(action: () -> Any?): String = try {
        action()
        "value"
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }

    private fun outcomeDouble(action: () -> Double): Any = try {
        action().toRawBits()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
