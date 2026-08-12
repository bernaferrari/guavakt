package com.bernaferrari.guavakt.math

/** Portable counterpart of Java's `RoundingMode` for the common numeric tier. */
enum class RoundingModeLike {
    UP,
    DOWN,
    CEILING,
    FLOOR,
    HALF_UP,
    HALF_DOWN,
    HALF_EVEN,
    UNNECESSARY,
}
