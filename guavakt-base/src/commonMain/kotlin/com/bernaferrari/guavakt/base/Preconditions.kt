package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
object Preconditions {
    fun checkArgument(expression: Boolean) {
        if (!expression) throw IllegalArgumentException()
    }

    fun checkArgument(expression: Boolean, errorMessage: Any?) {
        if (!expression) throw IllegalArgumentException(errorMessage?.toString())
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, vararg errorMessageArgs: Any?) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, *errorMessageArgs))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Char) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Int) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Long) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Any?) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Char, p2: Char) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Int, p2: Int) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Long, p2: Long) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3))
    }

    fun checkArgument(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        if (!expression) throw IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3, p4))
    }

    fun checkState(expression: Boolean) {
        if (!expression) throw IllegalStateException()
    }

    fun checkState(expression: Boolean, errorMessage: Any?) {
        if (!expression) throw IllegalStateException(errorMessage?.toString())
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, vararg errorMessageArgs: Any?) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, *errorMessageArgs))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Char) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Int) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Long) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Any?) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1, p2))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1, p2, p3))
    }

    fun checkState(expression: Boolean, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?, p4: Any?) {
        if (!expression) throw IllegalStateException(format(errorMessageTemplate, p1, p2, p3, p4))
    }

    fun <T> checkNotNull(reference: T?): T {
        if (reference == null) throw NullPointerException()
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessage: Any?): T {
        if (reference == null) throw NullPointerException(errorMessage?.toString())
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, vararg errorMessageArgs: Any?): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, *errorMessageArgs))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Char): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Int): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Long): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Any?): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Any?, p2: Any?): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1, p2))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1, p2, p3))
        return reference
    }

    fun <T> checkNotNull(reference: T?, errorMessageTemplate: String, p1: Any?, p2: Any?, p3: Any?, p4: Any?): T {
        if (reference == null) throw NullPointerException(format(errorMessageTemplate, p1, p2, p3, p4))
        return reference
    }

    /** Supplier-style message (Kotlin-friendly; Guava uses Object errorMessage). */
    inline fun <T> checkNotNull(reference: T?, lazyMessage: () -> Any?): T {
        if (reference == null) throw NullPointerException(lazyMessage()?.toString())
        return reference
    }

    fun checkElementIndex(index: Int, size: Int): Int = checkElementIndex(index, size, "index")

    fun checkElementIndex(index: Int, size: Int, desc: String): Int {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException(badElementIndex(index, size, desc))
        }
        return index
    }

    fun checkPositionIndex(index: Int, size: Int): Int = checkPositionIndex(index, size, "index")

    fun checkPositionIndex(index: Int, size: Int, desc: String): Int {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException(badPositionIndex(index, size, desc))
        }
        return index
    }

    fun checkPositionIndexes(start: Int, end: Int, size: Int) {
        if (start < 0 || end < start || end > size) {
            throw IndexOutOfBoundsException(badPositionIndexes(start, end, size))
        }
    }

    private fun badElementIndex(index: Int, size: Int, desc: String): String {
        if (index < 0) return format("%s (%s) must not be negative", desc, index)
        if (size < 0) throw IllegalArgumentException("negative size: $size")
        return format("%s (%s) must be less than size (%s)", desc, index, size)
    }

    private fun badPositionIndex(index: Int, size: Int, desc: String): String {
        if (index < 0) return format("%s (%s) must not be negative", desc, index)
        if (size < 0) throw IllegalArgumentException("negative size: $size")
        return format("%s (%s) must not be greater than size (%s)", desc, index, size)
    }

    private fun badPositionIndexes(start: Int, end: Int, size: Int): String {
        if (start < 0 || start > size) return badPositionIndex(start, size, "start index")
        if (end < 0 || end > size) return badPositionIndex(end, size, "end index")
        return format("end index (%s) must not be less than start index (%s)", end, start)
    }

    internal fun format(template: String, vararg errorMessageArgs: Any?): String {
        val builder = StringBuilder(template.length + 16 * errorMessageArgs.size)
        var templateStart = 0
        var i = 0
        while (i < errorMessageArgs.size) {
            val placeholderStart = template.indexOf("%s", templateStart)
            if (placeholderStart == -1) break
            builder.append(template, templateStart, placeholderStart)
            builder.append(errorMessageArgs[i++])
            templateStart = placeholderStart + 2
        }
        builder.append(template, templateStart, template.length)
        if (i < errorMessageArgs.size) {
            builder.append(" [")
            builder.append(errorMessageArgs[i++])
            while (i < errorMessageArgs.size) {
                builder.append(", ")
                builder.append(errorMessageArgs[i++])
            }
            builder.append(']')
        }
        return builder.toString()
    }
}
