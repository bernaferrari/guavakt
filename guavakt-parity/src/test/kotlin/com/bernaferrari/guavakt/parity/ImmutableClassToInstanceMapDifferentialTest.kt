package com.bernaferrari.guavakt.parity

import com.google.common.collect.ImmutableClassToInstanceMap as GuavaImmutableClassToInstanceMap
import com.bernaferrari.guavakt.collect.ImmutableClassToInstanceMap as GuavaKtImmutableClassToInstanceMap
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmutableClassToInstanceMapDifferentialTest {
    @Test
    fun singletonLookupCopyIdentityAndSnapshotMatchGuava() {
        val guava: GuavaImmutableClassToInstanceMap<Any> =
            GuavaImmutableClassToInstanceMap.of(String::class.java, "value")
        val guavaKt: GuavaKtImmutableClassToInstanceMap<Any> =
            GuavaKtImmutableClassToInstanceMap.of(String::class, "value")

        val guavaSource = linkedMapOf<Class<out Any>, Any>(String::class.java to "before")
        val guavaKtSource = linkedMapOf<KClass<out Any>, Any>(String::class to "before")
        val guavaSnapshot = GuavaImmutableClassToInstanceMap.copyOf<Any, Any>(guavaSource)
        val guavaKtSnapshot = GuavaKtImmutableClassToInstanceMap.copyOf(guavaKtSource)
        guavaSource[String::class.java] = "after"
        guavaKtSource[String::class] = "after"

        assertEquals(
            listOf(
                guava.getInstance(String::class.java), guava.getInstance(Int::class.javaObjectType),
                GuavaImmutableClassToInstanceMap.copyOf<Any, Any>(guava) === guava,
                guavaSnapshot.getInstance(String::class.java),
            ),
            listOf(
                guavaKt.getInstance(String::class), guavaKt.getInstance(Int::class),
                GuavaKtImmutableClassToInstanceMap.copyOf(guavaKt) === guavaKt,
                guavaKtSnapshot.getInstance(String::class),
            ),
        )
    }

    @Test
    fun builderOrderingDuplicatesPutAllAndReuseMatchGuava() {
        val guavaBuilder = GuavaImmutableClassToInstanceMap.builder<Any>()
            .put(String::class.java, "text")
            .put(Int::class.javaObjectType, 7)
        val guavaKtBuilder = GuavaKtImmutableClassToInstanceMap.builder<Any>()
            .put(String::class, "text")
            .put(Int::class, 7)
        val guavaFirst = guavaBuilder.build()
        val guavaKtFirst = guavaKtBuilder.build()
        guavaBuilder.put(Boolean::class.javaObjectType, true)
        guavaKtBuilder.put(Boolean::class, true)

        assertEquals(
            listOf(trace(guavaFirst), trace(guavaBuilder.build())),
            listOf(traceKt(guavaKtFirst), traceKt(guavaKtBuilder.build())),
        )

        val guavaDuplicate = GuavaImmutableClassToInstanceMap.builder<Any>()
            .put(String::class.java, "first").put(String::class.java, "second")
        val guavaKtDuplicate = GuavaKtImmutableClassToInstanceMap.builder<Any>()
            .put(String::class, "first").put(String::class, "second")
        assertEquals(failureName { guavaDuplicate.build() }, failureName { guavaKtDuplicate.build() })

        val guavaAll = linkedMapOf<Class<out Any>, Any>(String::class.java to "all", Int::class.javaObjectType to 3)
        val guavaKtAll = linkedMapOf<KClass<out Any>, Any>(String::class to "all", Int::class to 3)
        assertEquals(
            trace(GuavaImmutableClassToInstanceMap.builder<Any>().putAll(guavaAll).build()),
            traceKt(GuavaKtImmutableClassToInstanceMap.builder<Any>().putAll(guavaKtAll).build()),
        )
    }

    @Test
    fun forgedMappingValidationMatchesGuava() {
        val invalidGuava = linkedMapOf<Class<out Any>, Any>(String::class.java to 1)
        val invalidGuavaKt = linkedMapOf<KClass<out Any>, Any>(String::class to 1)
        assertEquals(
            failureName { GuavaImmutableClassToInstanceMap.copyOf<Any, Any>(invalidGuava) },
            failureName { GuavaKtImmutableClassToInstanceMap.copyOf(invalidGuavaKt) },
        )

        val partialGuava = linkedMapOf<Class<out Any>, Any>(
            String::class.java to "retained",
            Int::class.javaObjectType to "wrong",
        )
        val partialGuavaKt = linkedMapOf<KClass<out Any>, Any>(
            String::class to "retained",
            Int::class to "wrong",
        )
        val guavaBuilder = GuavaImmutableClassToInstanceMap.builder<Any>()
        val guavaKtBuilder = GuavaKtImmutableClassToInstanceMap.builder<Any>()
        assertEquals(
            listOf(failureName { guavaBuilder.putAll(partialGuava) }, trace(guavaBuilder.build())),
            listOf(failureName { guavaKtBuilder.putAll(partialGuavaKt) }, traceKt(guavaKtBuilder.build())),
        )
    }

    @Test
    fun directBulkAndNestedMutationFailuresMatchGuava() {
        val guava = GuavaImmutableClassToInstanceMap.builder<Any>()
            .put(String::class.java, "text").put(Int::class.javaObjectType, 7).build()
        val guavaKt = GuavaKtImmutableClassToInstanceMap.builder<Any>()
            .put(String::class, "text").put(Int::class, 7).build()

        assertEquals(
            listOf(
                failureName { guava.putInstance(String::class.java, "other") },
                failureName { guava[String::class.java] = "other" },
                failureName { guava.putAll(emptyMap()) },
                failureName { guava.remove(Boolean::class.javaObjectType) },
                failureName { guava.keys.remove(Boolean::class.javaObjectType) },
                failureName { guava.values.remove(false) },
                failureName { guava.entries.clear() },
                failureName { guava.entries.first().setValue("other") },
            ),
            listOf(
                failureName { guavaKt.putInstance(String::class, "other") },
                failureName { guavaKt[String::class] = "other" },
                failureName { guavaKt.putAll(emptyMap()) },
                failureName { guavaKt.remove(Boolean::class) },
                failureName { guavaKt.keys.remove(Boolean::class) },
                failureName { guavaKt.values.remove(false) },
                failureName { guavaKt.entries.clear() },
                failureName { guavaKt.entries.first().setValue("other") },
            ),
        )
    }

    private fun trace(map: GuavaImmutableClassToInstanceMap<Any>): List<Pair<String, Any>> =
        map.entries.map { it.key.kotlin.simpleName!! to it.value }

    private fun traceKt(map: GuavaKtImmutableClassToInstanceMap<Any>): List<Pair<String, Any>> =
        map.entries.map { it.key.simpleName!! to it.value }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }
}
