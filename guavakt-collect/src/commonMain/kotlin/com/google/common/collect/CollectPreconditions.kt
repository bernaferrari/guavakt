package dev.guavakt.collect

import dev.guavakt.base.Preconditions

object CollectPreconditions {
    fun checkEntryNotNull(key: Any?, value: Any?) {
        if (key == null) throw NullPointerException("null key in entry: null=$value")
        if (value == null) throw NullPointerException("null value in entry: $key=null")
    }

    fun checkNonnegative(value: Int, name: String): Int {
        if (value < 0) throw IllegalArgumentException("$name cannot be negative but was: $value")
        return value
    }

    fun checkNonnegative(value: Long, name: String): Long {
        if (value < 0) throw IllegalArgumentException("$name cannot be negative but was: $value")
        return value
    }

    fun checkPositive(value: Int, name: String): Int {
        if (value <= 0) throw IllegalArgumentException("$name must be positive but was: $value")
        return value
    }

    fun checkRemove(canRemove: Boolean) {
        Preconditions.checkState(canRemove, "no calls to next() since the last call to remove()")
    }
}
