package dev.guavakt.parity

import com.google.common.io.CharSource as GuavaCharSource
import dev.guavakt.io.CharSource as GuavaKtCharSource
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.test.Test
import kotlin.test.assertContentEquals

class CharSourceByteSourceDifferentialTest {
    @Test
    fun utf8ByteViewMatchesGuavaForSupplementaryCharacters() {
        listOf(
            "",
            "plain ASCII",
            "éclair",
            "A\uD83D\uDE00B",
            "\uD83D\uDE00\uD83D\uDE80",
            "unpaired-high-\uD83D",
            "unpaired-low-\uDE00",
        ).forEach { input ->
            assertContentEquals(
                GuavaCharSource.wrap(input).asByteSource(UTF_8).read(),
                GuavaKtCharSource.wrap(input).asByteSource().read(),
                input,
            )
        }
    }
}
