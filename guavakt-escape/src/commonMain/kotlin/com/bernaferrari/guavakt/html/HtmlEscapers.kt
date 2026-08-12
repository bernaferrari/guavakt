package com.bernaferrari.guavakt.html

import com.bernaferrari.guavakt.escape.CharEscaper
import com.bernaferrari.guavakt.escape.Escaper

object HtmlEscapers {
    private val HTML_ESCAPER: Escaper = object : CharEscaper() {
        override fun escape(c: Char): CharArray? = when (c) {
            '"' -> "&quot;".toCharArray()
            '\'' -> "&#39;".toCharArray()
            '&' -> "&amp;".toCharArray()
            '<' -> "&lt;".toCharArray()
            '>' -> "&gt;".toCharArray()
            else -> null
        }
    }
    fun htmlEscaper(): Escaper = HTML_ESCAPER
}
