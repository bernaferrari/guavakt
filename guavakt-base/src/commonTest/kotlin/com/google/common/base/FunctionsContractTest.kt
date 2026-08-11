package dev.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class FunctionsContractTest {
    @Test
    fun mapFunctionsDistinguishAbsentKeysFromPresentNullValues() {
        val map: Map<String, Int?> = linkedMapOf("value" to 7, "null" to null)
        val strict = Functions.forMap(map)
        val fallback = Functions.forMap(map, 9)

        assertEquals(7, strict.apply("value"))
        assertEquals(null, strict.apply("null"))
        assertFailsWith<IllegalArgumentException> { strict.apply("missing") }
        assertEquals(null, fallback.apply("null"))
        assertEquals(9, fallback.apply("missing"))
    }

    @Test
    fun sharedAndDerivedFunctionsHaveStableContracts() {
        assertSame(Functions.identity<String>(), Functions.identity<String>())
        assertSame(Functions.toStringFunction(), Functions.toStringFunction())
        assertEquals("Functions.identity()", Functions.identity<Int>().toString())
        assertEquals("Functions.toStringFunction()", Functions.toStringFunction().toString())
        assertFailsWith<NullPointerException> { Functions.toStringFunction().apply(null) }

        val constant = Functions.constant("value")
        assertEquals(constant, Functions.constant("value"))
        assertEquals("Functions.constant(value)", constant.toString())

        val predicate = Predicate<Int> { it > 0 }
        val function = Functions.forPredicate(predicate)
        assertEquals(true, function.apply(1))
        assertEquals(false, function.apply(0))
        assertEquals(function, Functions.forPredicate(predicate))

        val supplier = Supplier { 4 }
        assertEquals(4, Functions.forSupplier(supplier).apply(null))
        assertEquals(Functions.forSupplier(supplier), Functions.forSupplier(supplier))
    }
}
