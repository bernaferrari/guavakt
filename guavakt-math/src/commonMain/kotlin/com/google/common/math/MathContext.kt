package dev.guavakt.math

/**
 * Portable precision and rounding policy equivalent to `java.math.MathContext`.
 *
 * A precision of zero means unlimited exact precision. The predefined contexts use the same
 * precision and `HALF_EVEN` policy as the JDK decimal interchange contexts.
 */
class MathContext(
    val precision: Int,
    val roundingMode: RoundingMode = RoundingMode.HALF_UP,
) {
    init {
        require(precision >= 0) { "Digits < 0" }
    }

    override fun equals(other: Any?): Boolean =
        other is MathContext && precision == other.precision && roundingMode == other.roundingMode

    override fun hashCode(): Int = 31 * precision + roundingMode.hashCode()

    override fun toString(): String = "precision=$precision roundingMode=$roundingMode"

    companion object {
        val UNLIMITED = MathContext(0, RoundingMode.HALF_UP)
        val DECIMAL32 = MathContext(7, RoundingMode.HALF_EVEN)
        val DECIMAL64 = MathContext(16, RoundingMode.HALF_EVEN)
        val DECIMAL128 = MathContext(34, RoundingMode.HALF_EVEN)
    }
}
