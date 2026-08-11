package dev.guavakt.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Guava InternetDomainName / PSL behaviors used for real host routing. */
class GuavaPslFidelityTest {
    @Test fun co_uk_public_suffix() {
        val d = InternetDomainName.from("foo.bar.co.uk")
        assertEquals("co.uk", d.publicSuffix())
        assertEquals("bar.co.uk", d.topPrivateDomain().toString())
        assertTrue(d.isUnderPublicSuffix())
        assertFalse(d.isPublicSuffix())
    }

    @Test fun com_public_suffix() {
        val d = InternetDomainName.from("animals.example.com")
        assertEquals("com", d.publicSuffix())
        assertEquals("example.com", d.topPrivateDomain().toString())
    }

    @Test fun github_io_private() {
        // github.io is in PSL (often private)
        val d = InternetDomainName.from("shelter.github.io")
        assertEquals("github.io", d.publicSuffix())
        assertEquals("shelter.github.io", d.topPrivateDomain().toString())
    }
}
