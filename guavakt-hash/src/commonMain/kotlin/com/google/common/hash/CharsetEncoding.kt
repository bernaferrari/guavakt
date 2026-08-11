package dev.guavakt.hash

/** Charset encoding available consistently on every GuavaKt target. */
internal fun encodeString(input: CharSequence, charsetName: String): ByteArray {
    val text = input.toString()
    return when (charsetName.uppercase().replace('_', '-')) {
        "UTF-8", "UTF8" -> text.encodeToByteArray()
        "US-ASCII", "ASCII" -> ByteArray(text.length) { index ->
            val code = text[index].code
            require(code <= 0x7f) { "Character U+${code.toString(16)} is not US-ASCII" }
            code.toByte()
        }
        "ISO-8859-1", "ISO8859-1", "LATIN1" -> ByteArray(text.length) { index ->
            val code = text[index].code
            require(code <= 0xff) { "Character U+${code.toString(16)} is not ISO-8859-1" }
            code.toByte()
        }
        "UTF-16LE" -> utf16Bytes(text, littleEndian = true, bom = false)
        "UTF-16BE" -> utf16Bytes(text, littleEndian = false, bom = false)
        "UTF-16" -> utf16Bytes(text, littleEndian = false, bom = true)
        else -> throw IllegalArgumentException("Unsupported charset on Kotlin Multiplatform: $charsetName")
    }
}

private fun utf16Bytes(text: String, littleEndian: Boolean, bom: Boolean): ByteArray {
    val offset = if (bom) 2 else 0
    val bytes = ByteArray(offset + text.length * 2)
    if (bom) {
        bytes[0] = 0xfe.toByte()
        bytes[1] = 0xff.toByte()
    }
    for (index in text.indices) {
        val code = text[index].code
        val position = offset + index * 2
        if (littleEndian) {
            bytes[position] = code.toByte()
            bytes[position + 1] = (code ushr 8).toByte()
        } else {
            bytes[position] = (code ushr 8).toByte()
            bytes[position + 1] = code.toByte()
        }
    }
    return bytes
}
