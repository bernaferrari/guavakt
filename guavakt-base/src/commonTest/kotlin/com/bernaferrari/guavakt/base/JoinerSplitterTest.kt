package com.bernaferrari.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals

class JoinerSplitterTest {
    @Test
    fun joiner_joins() {
        assertEquals("a,b,c", Joiner.on(",").join(listOf("a", "b", "c")))
        assertEquals("a|null|c", Joiner.on("|").useForNull("null").join("a", null, "c"))
        assertEquals("a,c", Joiner.on(",").skipNulls().join(listOf("a", null, "c")))
    }

    @Test
    fun splitter_splits() {
        assertEquals(listOf("a", "b", "c"), Splitter.on(",").splitToList("a,b,c"))
        assertEquals(listOf("a", "b"), Splitter.on(",").omitEmptyStrings().splitToList("a,,b"))
        assertEquals(listOf("ab", "cd", "e"), Splitter.fixedLength(2).splitToList("abcde"))
        assertEquals(listOf("a", "b", "c"), Splitter.onPattern("\\s*,\\s*").splitToList("a, b , c"))
        assertEquals(listOf("a", "b", "c"), Splitter.on(CharMatcher.anyOf(",;")).splitToList("a,b;c"))
        assertEquals(listOf("a", "b;c"), Splitter.on(CharMatcher.anyOf(",;")).omitEmptyStrings().limit(2).splitToList(",a;b;c"))
    }

    @Test
    fun strings_padAndRepeat() {
        assertEquals("00x", Strings.padStart("x", 3, '0'))
        assertEquals("xxx", Strings.repeat("x", 3))
        assertEquals("ab", Strings.commonPrefix("abc", "abd"))
    }

    @Test
    fun caseFormat() {
        assertEquals("fooBar", CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, "foo_bar"))
        assertEquals("FOO_BAR", CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, "fooBar"))
        assertEquals("Foo", CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, "-foo"))
        assertEquals("foo", CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, "Foo"))
        assertEquals("u-r-l-value", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, "URLValue"))
        val converter = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.UPPER_CAMEL)
        assertEquals("FooBar", converter.convert("foo-bar"))
        assertEquals("foo-bar", converter.reverse().convert("FooBar"))
        assertEquals("LOWER_HYPHEN.converterTo(UPPER_CAMEL)", converter.toString())
    }
}
