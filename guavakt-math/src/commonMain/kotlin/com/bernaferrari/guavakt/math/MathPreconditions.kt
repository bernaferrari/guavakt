package com.bernaferrari.guavakt.math

internal object MathPreconditions {
    fun checkPositive(role: String, x: Int): Int {
        if (x <= 0) throw IllegalArgumentException("$role ($x) must be > 0")
        return x
    }

    fun checkPositive(role: String, x: Long): Long {
        if (x <= 0L) throw IllegalArgumentException("$role ($x) must be > 0")
        return x
    }

    fun checkNonNegative(role: String, x: Int): Int {
        if (x < 0) throw IllegalArgumentException("$role ($x) must be >= 0")
        return x
    }

    fun checkNonNegative(role: String, x: Long): Long {
        if (x < 0) throw IllegalArgumentException("$role ($x) must be >= 0")
        return x
    }

    fun checkRoundingUnnecessary(condition: Boolean) {
        if (!condition) throw ArithmeticException("mode was UNNECESSARY, but rounding was necessary")
    }
}
