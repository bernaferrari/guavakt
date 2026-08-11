package dev.guavakt.escape

import dev.guavakt.html.HtmlEscapers
import dev.guavakt.xml.XmlEscapers
import kotlin.test.Test
import kotlin.test.assertEquals

class EscapeTest {
    @Test
    fun htmlEscaper_escapesMarkup() {
        val out = HtmlEscapers.htmlEscaper().escape("""<a href="x">t&x</a>""")
        assertEquals("&lt;a href=&quot;x&quot;&gt;t&amp;x&lt;/a&gt;", out)
    }

    @Test
    fun xmlContentEscaper() {
        assertEquals("a&amp;b&lt;c", XmlEscapers.xmlContentEscaper().escape("a&b<c"))
    }

    @Test
    fun customEscaper() {
        val e = Escapers.builder().addEscape('a', "[A]").build()
        assertEquals("[A]b[A]", e.escape("aba"))
    }
}
