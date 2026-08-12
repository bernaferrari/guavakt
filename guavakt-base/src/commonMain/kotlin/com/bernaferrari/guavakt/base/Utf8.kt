package com.bernaferrari.guavakt.base

/**
 * Guava Utf8 helpers — UTF-8 well-formedness and encoded length (KMP).
 */
object Utf8 {
    fun encodedLength(sequence: CharSequence): Int {
        val utf16Length = sequence.length
        var utf8Length = utf16Length
        var i = 0
        while (i < utf16Length && sequence[i].code < 0x80) i++
        while (i < utf16Length) {
            val c = sequence[i].code
            if (c < 0x800) {
                utf8Length += (0x7f - c) ushr 31
            } else {
                utf8Length += encodedLengthGeneral(sequence, i)
                break
            }
            i++
        }
        if (utf8Length < utf16Length) {
            throw IllegalArgumentException("UTF-8 length does not fit in int: ${utf8Length + (1L shl 32)}")
        }
        return utf8Length
    }

    private fun encodedLengthGeneral(sequence: CharSequence, start: Int): Int {
        val utf16Length = sequence.length
        var utf8Length = 0
        var i = start
        while (i < utf16Length) {
            val c = sequence[i].code
            if (c < 0x800) {
                utf8Length += (0x7f - c) ushr 31
            } else {
                utf8Length += 2
                if (c in 0xd800..0xdfff) {
                    val codePoint = CharacterCodePointAt(sequence, i)
                    if (codePoint < 0x10000) throw IllegalArgumentException("Unpaired surrogate at index $i")
                    i++
                }
            }
            i++
        }
        return utf8Length
    }

    private fun CharacterCodePointAt(seq: CharSequence, index: Int): Int {
        val c1 = seq[index].code
        if (c1 !in 0xd800..0xdbff || index + 1 >= seq.length) return c1
        val c2 = seq[index + 1].code
        if (c2 !in 0xdc00..0xdfff) return c1
        return 0x10000 + ((c1 - 0xd800) shl 10) + (c2 - 0xdc00)
    }

    fun isWellFormed(bytes: ByteArray): Boolean = isWellFormed(bytes, 0, bytes.size)

    fun isWellFormed(bytes: ByteArray, off: Int, len: Int): Boolean {
        val end = off + len
        Preconditions.checkPositionIndexes(off, end, bytes.size)
        var i = off
        while (i < end) {
            val b = bytes[i].toInt() and 0xff
            when {
                b < 0x80 -> i++
                b < 0xc2 || b > 0xf4 -> return false
                b < 0xe0 -> {
                    if (i + 1 >= end) return false
                    val b2 = bytes[i + 1].toInt() and 0xff
                    if (b2 !in 0x80..0xbf) return false
                    i += 2
                }
                b < 0xf0 -> {
                    if (i + 2 >= end) return false
                    val b2 = bytes[i + 1].toInt() and 0xff
                    val b3 = bytes[i + 2].toInt() and 0xff
                    if (b2 !in 0x80..0xbf || b3 !in 0x80..0xbf) return false
                    if (b == 0xe0 && b2 < 0xa0) return false
                    if (b == 0xed && b2 > 0x9f) return false
                    i += 3
                }
                else -> {
                    if (i + 3 >= end) return false
                    val b2 = bytes[i + 1].toInt() and 0xff
                    val b3 = bytes[i + 2].toInt() and 0xff
                    val b4 = bytes[i + 3].toInt() and 0xff
                    if (b2 !in 0x80..0xbf || b3 !in 0x80..0xbf || b4 !in 0x80..0xbf) return false
                    if (b == 0xf0 && b2 < 0x90) return false
                    if (b == 0xf4 && b2 > 0x8f) return false
                    i += 4
                }
            }
        }
        return true
    }
}
