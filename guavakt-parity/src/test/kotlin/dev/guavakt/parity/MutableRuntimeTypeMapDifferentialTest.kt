package dev.guavakt.parity

import com.google.common.collect.MutableClassToInstanceMap as GuavaMutableClassToInstanceMap
import com.google.common.reflect.MutableTypeToInstanceMap as GuavaMutableTypeToInstanceMap
import com.google.common.reflect.TypeToken as GuavaTypeToken
import dev.guavakt.collect.MutableClassToInstanceMap as GuavaKtMutableClassToInstanceMap
import dev.guavakt.reflect.MutableTypeToInstanceMap as GuavaKtMutableTypeToInstanceMap
import dev.guavakt.reflect.TypeToken as GuavaKtTypeToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MutableRuntimeTypeMapDifferentialTest {
    @Test
    fun mutableClassMapTypedTraceAndRuntimeChecksMatchGuava() {
        val guava = GuavaMutableClassToInstanceMap.create<Any>()
        val guavaKt = GuavaKtMutableClassToInstanceMap.create<Any>()

        assertEquals(guava.putInstance(String::class.java, "one"), guavaKt.putInstance(String::class, "one"))
        assertEquals(guava.putInstance(String::class.java, "two"), guavaKt.putInstance(String::class, "two"))
        assertEquals(guava.getInstance(String::class.java), guavaKt.getInstance(String::class))

        assertFailsWith<ClassCastException> { guava[String::class.java] = 4 }
        assertFailsWith<ClassCastException> { guavaKt[String::class] = 4 }
        assertEquals("two", guava.getInstance(String::class.java))
        assertEquals("two", guavaKt.getInstance(String::class))

        assertFailsWith<ClassCastException> { guava.entries.first().setValue(4) }
        assertFailsWith<ClassCastException> { guavaKt.entries.first().setValue(4) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun mutableTypeMapRestrictedMutationRoutesMatchGuava() {
        val guava = GuavaMutableTypeToInstanceMap<Any>()
        val guavaKt = GuavaKtMutableTypeToInstanceMap<Any>()
        val guavaToken = GuavaTypeToken.of(String::class.java)
        val guavaKtToken = GuavaKtTypeToken.of(String::class)

        assertEquals(guava.putInstance(guavaToken, "one"), guavaKt.putInstance(guavaKtToken, "one"))
        assertEquals(guava.getInstance(guavaToken), guavaKt.getInstance(guavaKtToken))
        assertFailsWith<UnsupportedOperationException> { guava.put(guavaToken, "two") }
        assertFailsWith<UnsupportedOperationException> { guavaKt.put(guavaKtToken, "two") }
        assertFailsWith<UnsupportedOperationException> { guava.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> { guavaKt.putAll(emptyMap()) }
        assertFailsWith<UnsupportedOperationException> { guava.entries.first().setValue("two") }
        assertFailsWith<UnsupportedOperationException> { guavaKt.entries.first().setValue("two") }

        guava.entries.iterator().also { it.next(); it.remove() }
        guavaKt.entries.iterator().also { it.next(); it.remove() }
        assertEquals(guava.isEmpty(), guavaKt.isEmpty())
    }
}
