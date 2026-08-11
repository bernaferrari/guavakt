package dev.guavakt.base

/**
 * Guava Charsets — charset *names* for KMP (no java.nio.Charset on all targets).
 * Use with platform encoders; string constants match Guava field names.
 */
object Charsets {
    const val US_ASCII_NAME = "US-ASCII"
    const val ISO_8859_1_NAME = "ISO-8859-1"
    const val UTF_8_NAME = "UTF-8"
    const val UTF_16BE_NAME = "UTF-16BE"
    const val UTF_16LE_NAME = "UTF-16LE"
    const val UTF_16_NAME = "UTF-16"

    /** Guava-shaped aliases (name strings, not Charset objects). */
    val US_ASCII: String get() = US_ASCII_NAME
    val ISO_8859_1: String get() = ISO_8859_1_NAME
    val UTF_8: String get() = UTF_8_NAME
    val UTF_16BE: String get() = UTF_16BE_NAME
    val UTF_16LE: String get() = UTF_16LE_NAME
    val UTF_16: String get() = UTF_16_NAME

    fun isUtf8Name(name: String): Boolean =
        name.equals(UTF_8_NAME, ignoreCase = true) || name.equals("UTF8", ignoreCase = true)
}
