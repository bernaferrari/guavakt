package com.bernaferrari.guavakt.parity

import com.bernaferrari.guavakt.math.BigInteger as GuavaKtBigInteger
import java.math.BigInteger
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BigIntegerValueDifferentialTest {
    @Test
    fun radixByteAndPrimitiveConversionsMatchTheJdk() {
        val values = listOf(
            "0", "1", "-1", "127", "128", "255", "256", "-128", "-129",
            "9223372036854775808", "-9223372036854775809",
            "12345678901234567890123456789012345678901234567890",
        )
        for (source in values) {
            val java = BigInteger(source)
            val kotlin = GuavaKtBigInteger.parse(source)
            assertContentEquals(java.toByteArray(), kotlin.toByteArray(), "bytes $source")
            assertEquals(java.toString(), GuavaKtBigInteger.fromByteArray(java.toByteArray()).toString(), "parse bytes $source")
            assertEquals(java.toInt(), kotlin.toInt(), "int $source")
            assertEquals(java.toLong(), kotlin.toLong(), "long $source")
            assertEquals(java.toDouble().toRawBits(), kotlin.toDouble().toRawBits(), "double $source")
            assertEquals(java.toFloat().toRawBits(), kotlin.toFloat().toRawBits(), "float $source")
            for (radix in listOf(2, 3, 8, 10, 16, 36)) {
                assertEquals(java.toString(radix), kotlin.toString(radix), "radix $radix $source")
                assertEquals(java.toString(), GuavaKtBigInteger.parse(java.toString(radix), radix).toString(), "parse radix $radix $source")
            }
        }
    }

    @Test
    fun twoComplementAndShiftOperationsMatchTheJdk() {
        val sources = listOf("-257", "-129", "-128", "-3", "-2", "-1", "0", "1", "2", "127", "128", "255", "256", "257", "12345678901234567890")
        for (leftSource in sources) {
            val javaLeft = BigInteger(leftSource)
            val kotlinLeft = GuavaKtBigInteger.parse(leftSource)
            assertEquals(javaLeft.bitLength(), kotlinLeft.bitLength(), "bitLength $leftSource")
            assertEquals(javaLeft.bitCount(), kotlinLeft.bitCount(), "bitCount $leftSource")
            assertEquals(javaLeft.lowestSetBit, kotlinLeft.getLowestSetBit(), "lowest bit $leftSource")
            for (distance in listOf(-8, -1, 0, 1, 2, 7, 8, 31, 64)) {
                assertEquals(javaLeft.shiftLeft(distance).toString(), kotlinLeft.shiftLeft(distance).toString(), "left $leftSource $distance")
                assertEquals(javaLeft.shiftRight(distance).toString(), kotlinLeft.shiftRight(distance).toString(), "right $leftSource $distance")
            }
            for (bit in 0..80) assertEquals(javaLeft.testBit(bit), kotlinLeft.testBit(bit), "test $leftSource $bit")
            for (rightSource in sources) {
                val javaRight = BigInteger(rightSource)
                val kotlinRight = GuavaKtBigInteger.parse(rightSource)
                assertEquals(javaLeft.and(javaRight).toString(), (kotlinLeft and kotlinRight).toString(), "and $leftSource $rightSource")
                assertEquals(javaLeft.or(javaRight).toString(), (kotlinLeft or kotlinRight).toString(), "or $leftSource $rightSource")
                assertEquals(javaLeft.xor(javaRight).toString(), (kotlinLeft xor kotlinRight).toString(), "xor $leftSource $rightSource")
                assertEquals(javaLeft.andNot(javaRight).toString(), kotlinLeft.andNot(kotlinRight).toString(), "andNot $leftSource $rightSource")
            }
            for (bit in listOf(0, 1, 7, 8, 63)) {
                assertEquals(javaLeft.setBit(bit).toString(), kotlinLeft.setBit(bit).toString(), "set $leftSource $bit")
                assertEquals(javaLeft.clearBit(bit).toString(), kotlinLeft.clearBit(bit).toString(), "clear $leftSource $bit")
                assertEquals(javaLeft.flipBit(bit).toString(), kotlinLeft.flipBit(bit).toString(), "flip $leftSource $bit")
            }
            assertEquals(javaLeft.not().toString(), kotlinLeft.not().toString(), "not $leftSource")
        }
    }

    @Test
    fun modularRootsAndPrimeOperationsCoverArbitraryValues() {
        val random = Random(20260811)
        repeat(24) {
            val leftJava = BigInteger(256, java.util.Random(random.nextLong()))
            val modulusJava = BigInteger.probablePrime(127, java.util.Random(random.nextLong()))
            val exponentJava = BigInteger(80, java.util.Random(random.nextLong()))
            val left = GuavaKtBigInteger.parse(leftJava.toString())
            val modulus = GuavaKtBigInteger.parse(modulusJava.toString())
            val exponent = GuavaKtBigInteger.parse(exponentJava.toString())
            assertEquals(leftJava.modPow(exponentJava, modulusJava).toString(), left.modPow(exponent, modulus).toString(), "modPow $it")
            if (leftJava.gcd(modulusJava) == BigInteger.ONE) {
                assertEquals(leftJava.modInverse(modulusJava).toString(), left.modInverse(modulus).toString(), "inverse $it")
            }
        }
        for (source in listOf("0", "1", "2", "3", "4", "9", "10", "99980001", "123456789012345678901234567890")) {
            val java = BigInteger(source)
            val kotlin = GuavaKtBigInteger.parse(source)
            assertEquals(java.sqrt().toString(), kotlin.sqrt().toString(), "sqrt $source")
            val kotlinRootAndRemainder = kotlin.sqrtAndRemainder()
            assertEquals(
                java.sqrtAndRemainder().joinToString(",") { it.toString() },
                listOf(kotlinRootAndRemainder.first, kotlinRootAndRemainder.second).joinToString(","),
                "sqrt/remainder $source",
            )
        }
        val prime = GuavaKtBigInteger.probablePrime(96, Random(17))
        assertEquals(96, prime.bitLength())
        assertTrue(BigInteger(prime.toString()).isProbablePrime(100))
        assertEquals("101", GuavaKtBigInteger.of(100).nextProbablePrime().toString())
    }
}
