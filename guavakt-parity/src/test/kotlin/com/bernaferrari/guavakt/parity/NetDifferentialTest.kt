package com.bernaferrari.guavakt.parity

import com.google.common.net.HostSpecifier as GuavaHostSpecifier
import com.bernaferrari.guavakt.net.HostSpecifier
import kotlin.test.Test
import kotlin.test.assertEquals

class NetDifferentialTest {
    @Test fun hostSpecifierRejectsPortsAndDomainsWithoutPublicSuffix() {
        for (value in listOf("example.com:443", "localhost", "not-a-public-suffix.invalid")) {
            assertEquals(runCatching { GuavaHostSpecifier.fromValid(value) }.isSuccess,
                runCatching { HostSpecifier.fromValid(value) }.isSuccess, value)
        }
    }

    @Test fun hostSpecifierCanonicalizesIpLiteralsLikeGuava() {
        for (value in listOf("127.0.0.1", "[2001:db8::1]", "2001:db8::1")) {
            assertEquals(GuavaHostSpecifier.fromValid(value).toString(), HostSpecifier.fromValid(value).toString())
        }
    }
}
