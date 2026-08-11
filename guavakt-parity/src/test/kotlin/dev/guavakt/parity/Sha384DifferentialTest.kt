package dev.guavakt.parity

import com.google.common.hash.Hashing as GuavaHashing
import dev.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha384DifferentialTest {
    @Test
    fun sha384MatchesGuavaAcrossPaddingBoundaries() {
        for (length in listOf(0, 1, 3, 111, 112, 113, 127, 128, 129, 257)) {
            val bytes = ByteArray(length) { index -> (index * 73 + 19).toByte() }
            assertEquals(
                GuavaHashing.sha384().hashBytes(bytes).toString(),
                GuavaKtHashing.sha384().hashBytes(bytes).toString(),
                "length=$length",
            )
        }
    }
}
