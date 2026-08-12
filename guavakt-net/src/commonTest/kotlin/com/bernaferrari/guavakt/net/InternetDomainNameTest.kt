package com.bernaferrari.guavakt.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InternetDomainNameTest {
    @Test
    fun publicSuffix_and_topPrivateDomain() {
        val name = InternetDomainName.from("foo.bar.co.uk")
        assertEquals("co.uk", name.publicSuffix())
        assertTrue(name.isUnderPublicSuffix())
        assertEquals("bar.co.uk", name.topPrivateDomain().toString())
    }
}
