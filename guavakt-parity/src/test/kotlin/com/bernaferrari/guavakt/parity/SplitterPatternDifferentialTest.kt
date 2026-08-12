package com.bernaferrari.guavakt.parity

import com.google.common.base.Splitter as GuavaSplitter
import com.bernaferrari.guavakt.base.Splitter
import kotlin.test.Test
import kotlin.test.assertEquals

class SplitterPatternDifferentialTest {
    @Test
    fun nonEmptyRegexPatternsRespectOmissionTrimmingAndLimits() {
        val cases = listOf(
            "\\s*,\\s*" to " a, b , , c ",
            "[,:]+" to "a,,b:c:::d",
            "\\d+" to "a12b003c",
            "(?=a)" to "baac",
            "(?=a)" to "aab",
        )
        for ((pattern, input) in cases) {
            assertEquals(
                GuavaSplitter.onPattern(pattern).splitToList(input),
                Splitter.onPattern(pattern).splitToList(input),
                "pattern=$pattern input=$input",
            )
            assertEquals(
                GuavaSplitter.onPattern(pattern).trimResults().omitEmptyStrings().limit(2).splitToList(input),
                Splitter.onPattern(pattern).trimResults().omitEmptyStrings().limit(2).splitToList(input),
                "configured pattern=$pattern input=$input",
            )
        }
    }

    @Test
    fun emptyMatchingPatternsAreRejectedLikeGuava() {
        for (pattern in listOf("", "a*", "^", "(?=$)")) {
            assertEquals(
                outcome { GuavaSplitter.onPattern(pattern) },
                outcome { Splitter.onPattern(pattern) },
                "pattern=$pattern",
            )
        }
    }

    private fun <T> outcome(action: () -> T): String = try {
        action()
        "value"
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
