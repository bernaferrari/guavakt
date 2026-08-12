package com.bernaferrari.guavakt.xml

import com.bernaferrari.guavakt.escape.CharEscaper
import com.bernaferrari.guavakt.escape.Escaper

object XmlEscapers {
    private val XML_CONTENT: Escaper = object : CharEscaper() {
        override fun escape(c: Char): CharArray? = when (c) {
            '&' -> "&amp;".toCharArray()
            '<' -> "&lt;".toCharArray()
            '>' -> "&gt;".toCharArray()
            '\r', '\n', '\t' -> null
            else -> if (c.code < 0x20) "&#${c.code};".toCharArray() else null
        }
    }
    private val XML_ATTR: Escaper = object : CharEscaper() {
        override fun escape(c: Char): CharArray? = when (c) {
            '"' -> "&quot;".toCharArray()
            '\'' -> "&apos;".toCharArray()
            '&' -> "&amp;".toCharArray()
            '<' -> "&lt;".toCharArray()
            '\t' -> "&#x9;".toCharArray()
            '\n' -> "&#xA;".toCharArray()
            '\r' -> "&#xD;".toCharArray()
            else -> if (c.code < 0x20) "&#${c.code};".toCharArray() else null
        }
    }
    fun xmlContentEscaper(): Escaper = XML_CONTENT
    fun xmlAttributeEscaper(): Escaper = XML_ATTR
}
