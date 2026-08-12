package com.bernaferrari.guavakt.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FinalizableReferenceContractTest {
    @Test fun phantomReferenceNeverExposesReferent() {
        val queue = FinalizableReferenceQueue()
        val reference = FinalizablePhantomReference(Any(), queue)
        assertNull(reference.get())
        reference.clear()
        assertNull(reference.get())
    }

    @Test fun subclassesCanCustomizeFinalization() {
        val queue = FinalizableReferenceQueue()
        val reference = object : FinalizableWeakReference<Any>(Any(), queue) {
            var finalizationCount = 0

            override fun finalizeReferent() {
                finalizationCount++
                super.finalizeReferent()
            }
        }

        reference.finalizeReferent()
        assertEquals(1, reference.finalizationCount)
        assertNull(reference.get())
        queue.close()
    }
}
