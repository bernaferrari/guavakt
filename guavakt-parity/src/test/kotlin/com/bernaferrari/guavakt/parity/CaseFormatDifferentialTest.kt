package com.bernaferrari.guavakt.parity

import com.google.common.base.CaseFormat as GuavaCaseFormat
import com.bernaferrari.guavakt.base.CaseFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class CaseFormatDifferentialTest {
    @Test
    fun conversionMatchesGuavaForSeparatorsCamelBoundariesAcronymsAndEmptyWords() {
        val inputs = listOf(
            "",
            "foo",
            "Foo",
            "FOO",
            "fooBar",
            "FooBar",
            "URLValue",
            "foo2Bar",
            "foo_bar",
            "FOO_BAR",
            "foo-bar",
            "foo__bar",
            "_foo",
            "foo_",
            "foo--bar",
            "-foo",
            "foo-",
        )
        for (source in CaseFormat.entries) {
            val guavaSource = GuavaCaseFormat.valueOf(source.name)
            for (target in CaseFormat.entries) {
                val guavaTarget = GuavaCaseFormat.valueOf(target.name)
                for (input in inputs) {
                    assertEquals(
                        outcome { guavaSource.to(guavaTarget, input) },
                        outcome { source.to(target, input) },
                        "$source -> $target input='$input'",
                    )
                }
            }
        }
    }

    @Test
    fun reusableConvertersMatchGuavaForwardBackwardIdentityAndPresentation() {
        val guava = GuavaCaseFormat.LOWER_HYPHEN.converterTo(GuavaCaseFormat.UPPER_CAMEL)
        val ours = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.UPPER_CAMEL)

        assertEquals(
            listOf("foo-bar", "-foo", "foo--bar").map(guava::convert),
            listOf("foo-bar", "-foo", "foo--bar").map(ours::convert),
        )
        assertEquals(guava.reverse().convert("FooBar"), ours.reverse().convert("FooBar"))
        assertEquals(guava.toString(), ours.toString())
        assertEquals(
            guava == GuavaCaseFormat.LOWER_HYPHEN.converterTo(GuavaCaseFormat.UPPER_CAMEL),
            ours == CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.UPPER_CAMEL),
        )
        assertEquals(
            ours.hashCode(),
            CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.UPPER_CAMEL).hashCode(),
        )
    }

    private fun <T> outcome(action: () -> T): Any? = try {
        action()
    } catch (failure: Throwable) {
        failure::class.simpleName ?: "unknown"
    }
}
