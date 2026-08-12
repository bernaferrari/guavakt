package com.bernaferrari.guavakt.net

import com.bernaferrari.guavakt.escape.Escaper

class PercentEscaper(
    safeChars: String,
    private val plusForSpace: Boolean,
) : Escaper() {
    private val safeOctets = BooleanArray(128).also { arr ->
        for (c in '0'..'9') arr[c.code] = true
        for (c in 'A'..'Z') arr[c.code] = true
        for (c in 'a'..'z') arr[c.code] = true
        for (c in safeChars) if (c.code < 128) arr[c.code] = true
    }

    override fun escape(string: String): String = buildString(string.length * 2) {
        for (ch in string) {
            val c = ch.code
            when {
                c < 128 && safeOctets[c] -> append(ch)
                c == ' '.code && plusForSpace -> append('+')
                c < 0x80 -> {
                    append('%')
                    append(HEX[c ushr 4])
                    append(HEX[c and 0xf])
                }
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        val v = b.toInt() and 0xff
                        append('%')
                        append(HEX[v ushr 4])
                        append(HEX[v and 0xf])
                    }
                }
            }
        }
    }

    companion object {
        private val HEX = "0123456789ABCDEF".toCharArray()
    }
}
