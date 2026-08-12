package com.bernaferrari.guavakt.parity

import com.google.common.hash.Hashing as GuavaHashing
import com.bernaferrari.guavakt.hash.Hashing as GuavaKtHashing
import kotlin.test.Test
import kotlin.test.assertEquals

class Fingerprint2011DifferentialTest {
    @Test
    fun fingerprint2011MatchesGuavaAcrossEveryAlgorithmBoundary() {
        for (length in listOf(0, 1, 7, 8, 9, 31, 32, 33, 63, 64, 65, 127, 128, 129, 1024)) {
            val bytes = ByteArray(length) { index -> (index * 73 + 19).toByte() }
            assertEquals(
                GuavaHashing.fingerprint2011().hashBytes(bytes).toString(),
                GuavaKtHashing.fingerprint2011().hashBytes(bytes).toString(),
                "length=$length",
            )
        }
    }
}
