package dev.guavakt.parity

import com.google.common.base.Functions as GuavaFunctions
import dev.guavakt.base.Functions as GuavaKtFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class FunctionsDifferentialTest {
    @Test
    fun mapNullAndMissingKeyContractsMatchGuava() {
        val map: Map<String, Int?> = linkedMapOf("value" to 7, "null" to null)
        val guavaStrict = GuavaFunctions.forMap(map)
        val kotlinStrict = GuavaKtFunctions.forMap(map)
        val guavaFallback = GuavaFunctions.forMap(map, 9)
        val kotlinFallback = GuavaKtFunctions.forMap(map, 9)

        for (key in listOf("value", "null")) {
            assertEquals(guavaStrict.apply(key), kotlinStrict.apply(key))
            assertEquals(guavaFallback.apply(key), kotlinFallback.apply(key))
        }
        assertFailsWith<IllegalArgumentException> { guavaStrict.apply("missing") }
        assertFailsWith<IllegalArgumentException> { kotlinStrict.apply("missing") }
        assertEquals(guavaFallback.apply("missing"), kotlinFallback.apply("missing"))
    }

    @Test
    fun singletonAndCompositionResultsMatchGuava() {
        assertSame(GuavaFunctions.identity<Any>(), GuavaFunctions.identity<Any>())
        assertSame(GuavaKtFunctions.identity<Any>(), GuavaKtFunctions.identity<Any>())
        assertSame(GuavaFunctions.toStringFunction(), GuavaFunctions.toStringFunction())
        assertSame(GuavaKtFunctions.toStringFunction(), GuavaKtFunctions.toStringFunction())
        assertEquals(GuavaFunctions.toStringFunction().apply(42), GuavaKtFunctions.toStringFunction().apply(42))
        assertFailsWith<NullPointerException> { GuavaKtFunctions.toStringFunction().apply(null) }

        val guavaMap = GuavaFunctions.forMap(mapOf("one" to "1"))
        val kotlinMap = GuavaKtFunctions.forMap(mapOf("one" to "1"))
        assertEquals(guavaMap.apply("one"), kotlinMap.apply("one"))
    }
}
