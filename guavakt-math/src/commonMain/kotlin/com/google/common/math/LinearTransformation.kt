package dev.guavakt.math

import dev.guavakt.base.Preconditions

/**
 * Guava LinearTransformation — mapping y = m*x + b (or vertical).
 */
abstract class LinearTransformation {
    abstract fun isVertical(): Boolean
    abstract fun isHorizontal(): Boolean
    abstract fun slope(): Double
    abstract fun transform(x: Double): Double
    abstract fun inverse(): LinearTransformation

    companion object {
        fun mapping(x1: Double, y1: Double): LinearTransformationBuilder {
            Preconditions.checkArgument(x1.isFinite())
            Preconditions.checkArgument(y1.isFinite())
            return LinearTransformationBuilder(x1, y1)
        }

        fun vertical(x: Double): LinearTransformation {
            Preconditions.checkArgument(x.isFinite())
            return VerticalLinearTransformation(x)
        }

        fun horizontal(y: Double): LinearTransformation {
            Preconditions.checkArgument(y.isFinite())
            return RegularLinearTransformation(0.0, y)
        }
        fun forNaN(): LinearTransformation = NaNLinearTransformation
    }

    class LinearTransformationBuilder internal constructor(
        private val x1: Double,
        private val y1: Double,
    ) {
        fun and(x2: Double, y2: Double): LinearTransformation {
            Preconditions.checkArgument(x2.isFinite())
            Preconditions.checkArgument(y2.isFinite())
            if (x1 == x2) {
                Preconditions.checkArgument(y1 != y2)
                return vertical(x1)
            }
            val slope = (y2 - y1) / (x2 - x1)
            return withSlope(slope)
        }
        fun withSlope(slope: Double): LinearTransformation {
            Preconditions.checkArgument(!slope.isNaN())
            if (slope.isInfinite()) return vertical(x1)
            val yIntercept = y1 - x1 * slope
            return RegularLinearTransformation(slope, yIntercept)
        }
    }

    private class RegularLinearTransformation(
        private val slope: Double,
        private val yIntercept: Double,
    ) : LinearTransformation() {
        override fun isVertical(): Boolean = false
        override fun isHorizontal(): Boolean = slope == 0.0
        override fun slope(): Double = slope
        override fun transform(x: Double): Double = x * slope + yIntercept
        override fun inverse(): LinearTransformation {
            if (slope == 0.0) return vertical(yIntercept)
            return RegularLinearTransformation(1.0 / slope, -yIntercept / slope)
        }
    }

    private class VerticalLinearTransformation(private val x: Double) : LinearTransformation() {
        override fun isVertical(): Boolean = true
        override fun isHorizontal(): Boolean = false
        override fun slope(): Double = throw IllegalStateException("vertical")
        override fun transform(x: Double): Double = throw IllegalStateException("vertical")
        override fun inverse(): LinearTransformation = horizontal(x)
    }

    private object NaNLinearTransformation : LinearTransformation() {
        override fun isVertical(): Boolean = false
        override fun isHorizontal(): Boolean = false
        override fun slope(): Double = Double.NaN
        override fun transform(x: Double): Double = Double.NaN
        override fun inverse(): LinearTransformation = this
    }
}
