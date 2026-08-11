package dev.guavakt.annotations

import kotlin.test.Test
import kotlin.test.assertNotNull

class AnnotationsPresentTest {
    @Test
    fun annotations_areLoadable() {
        assertNotNull(GwtCompatible::class.simpleName)
        assertNotNull(Beta::class.simpleName)
    }
}
