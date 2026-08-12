package com.bernaferrari.guavakt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LinearTransformationTest {
    @Test
    fun regularAndVerticalTransformationsKeepTheirDistinctContracts() {
        val regular = LinearTransformation.mapping(1.0, 2.0).and(3.0, 6.0)
        assertEquals(2.0, regular.slope())
        assertEquals(8.0, regular.transform(4.0))
        assertEquals(4.0, regular.inverse().transform(8.0))

        val vertical = LinearTransformation.vertical(3.0)
        assertTrue(vertical.isVertical())
        assertFailsWith<IllegalStateException> { vertical.slope() }
        assertFailsWith<IllegalStateException> { vertical.transform(3.0) }
        assertEquals(3.0, vertical.inverse().transform(0.0))
    }

    @Test
    fun coordinatesMustBeFiniteButInfiniteSlopeRepresentsVertical() {
        assertFailsWith<IllegalArgumentException> { LinearTransformation.mapping(Double.NaN, 1.0) }
        assertFailsWith<IllegalArgumentException> { LinearTransformation.horizontal(Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { LinearTransformation.vertical(Double.NEGATIVE_INFINITY) }
        assertTrue(
            LinearTransformation.mapping(1.0, 2.0)
                .withSlope(Double.POSITIVE_INFINITY)
                .isVertical(),
        )
    }
}
