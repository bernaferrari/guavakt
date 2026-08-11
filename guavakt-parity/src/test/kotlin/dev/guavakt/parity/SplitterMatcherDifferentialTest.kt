package dev.guavakt.parity

import com.google.common.base.CharMatcher as GuavaCharMatcher
import com.google.common.base.Splitter as GuavaSplitter
import dev.guavakt.base.CharMatcher
import dev.guavakt.base.Splitter
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitterMatcherDifferentialTest {
    @Test
    fun matcherSeparatorsMatchGuavaAcrossAdjacentEdgesAndConfiguredSplits() {
        val inputs = listOf("", ",", ",a;", "a,,b;;;c", " a ; ; b , c ")
        for (input in inputs) {
            assertEquals(
                GuavaSplitter.on(GuavaCharMatcher.anyOf(",;")).splitToList(input),
                Splitter.on(CharMatcher.anyOf(",;")).splitToList(input),
                "input=$input",
            )
            assertEquals(
                GuavaSplitter.on(GuavaCharMatcher.anyOf(",;")).trimResults().omitEmptyStrings().limit(2).splitToList(input),
                Splitter.on(CharMatcher.anyOf(",;")).trimResults().omitEmptyStrings().limit(2).splitToList(input),
                "configured input=$input",
            )
        }
    }
}
