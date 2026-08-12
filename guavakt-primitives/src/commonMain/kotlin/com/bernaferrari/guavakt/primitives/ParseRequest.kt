package com.bernaferrari.guavakt.primitives

/**
 * Guava ParseRequest — parses radix prefixes (0x, 0, #) for unsigned integer parsing.
 */
internal class ParseRequest private constructor(
    val rawValue: String,
    val radix: Int,
) {
    companion object {
        fun fromString(stringValue: String): ParseRequest {
            if (stringValue.isEmpty()) throw NumberFormatException("empty string")
            val first = stringValue[0]
            return when {
                stringValue.startsWith("0x") || stringValue.startsWith("0X") ->
                    ParseRequest(stringValue.substring(2), 16)
                stringValue.startsWith("#") ->
                    ParseRequest(stringValue.substring(1), 16)
                first == '0' && stringValue.length > 1 ->
                    ParseRequest(stringValue.substring(1), 8)
                else -> ParseRequest(stringValue, 10)
            }
        }
    }
}
