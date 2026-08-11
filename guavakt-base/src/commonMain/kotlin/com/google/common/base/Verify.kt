package dev.guavakt.base

import dev.guavakt.annotations.GwtCompatible

@GwtCompatible
object Verify {
    fun verify(expression: Boolean) {
        if (!expression) throw VerifyException()
    }

    fun verify(expression: Boolean, errorMessageTemplate: String, vararg errorMessageArgs: Any?) {
        if (!expression) throw VerifyException(Preconditions.format(errorMessageTemplate, *errorMessageArgs))
    }

    fun <T> verifyNotNull(reference: T?): T {
        if (reference == null) throw VerifyException("expected a non-null reference")
        return reference
    }

    fun <T> verifyNotNull(reference: T?, errorMessageTemplate: String, vararg errorMessageArgs: Any?): T {
        if (reference == null) {
            throw VerifyException(Preconditions.format(errorMessageTemplate, *errorMessageArgs))
        }
        return reference
    }
}
