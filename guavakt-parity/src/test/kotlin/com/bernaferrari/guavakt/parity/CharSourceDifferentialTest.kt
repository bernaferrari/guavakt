package com.bernaferrari.guavakt.parity

import com.google.common.io.CharSource as GuavaCharSource
import com.bernaferrari.guavakt.io.CharSource as GuavaKtCharSource
import kotlin.test.Test
import kotlin.test.assertEquals

class CharSourceDifferentialTest {
    @Test
    fun lineEndingAndFirstLineRulesMatch() {
        val value = "first\rsecond\r\nthird\nfourth\n\rfinal"
        val guava = GuavaCharSource.wrap(value)
        val guavaKt = GuavaKtCharSource.wrap(value)

        assertEquals(guava.readFirstLine(), guavaKt.readFirstLine())
        assertEquals(guava.readLines(), guavaKt.readLines())
    }

    @Test
    fun concatenationReadLengthAndEmptyBehaviorMatch() {
        val guava = GuavaCharSource.concat(
            GuavaCharSource.empty(),
            GuavaCharSource.wrap("ab"),
            GuavaCharSource.empty(),
            GuavaCharSource.wrap("😀"),
        )
        val guavaKt = GuavaKtCharSource.concat(
            GuavaKtCharSource.empty(),
            GuavaKtCharSource.wrap("ab"),
            GuavaKtCharSource.empty(),
            GuavaKtCharSource.wrap("😀"),
        )

        assertEquals(guava.read(), guavaKt.read())
        assertEquals(guava.length(), guavaKt.length())
        assertEquals(guava.isEmpty, guavaKt.isEmpty())
        assertEquals(GuavaCharSource.concat().isEmpty, GuavaKtCharSource.concat().isEmpty())
    }

    @Test
    fun knownLengthsAndCopyToAppendableMatch() {
        val value = "ab😀"
        val guava = GuavaCharSource.concat(GuavaCharSource.wrap("ab"), GuavaCharSource.wrap("😀"))
        val guavaKt = GuavaKtCharSource.concat(GuavaKtCharSource.wrap("ab"), GuavaKtCharSource.wrap("😀"))
        val guavaOutput = StringBuilder()
        val guavaKtOutput = StringBuilder()

        assertEquals(guava.lengthIfKnown().orNull(), guavaKt.lengthIfKnown())
        assertEquals(guava.copyTo(guavaOutput), guavaKt.copyTo(guavaKtOutput))
        assertEquals(value, guavaKtOutput.toString())
    }

    @Test
    fun iterableConcat_matchesGuava() {
        val guava = GuavaCharSource.concat(listOf(GuavaCharSource.wrap("a"), GuavaCharSource.wrap("b")))
        val guavaKt = GuavaKtCharSource.concat(listOf(GuavaKtCharSource.wrap("a"), GuavaKtCharSource.wrap("b")))

        assertEquals(guava.read(), guavaKt.read())
        assertEquals(guava.lengthIfKnown().orNull(), guavaKt.lengthIfKnown())
    }
}
