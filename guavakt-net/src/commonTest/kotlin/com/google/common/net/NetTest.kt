package dev.guavakt.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetTest {
    @Test
    fun hostAndPort_parse() {
        val hp = HostAndPort.fromString("example.com:8080")
        assertEquals("example.com", hp.getHost())
        assertEquals(8080, hp.getPort())
        val br = HostAndPort.fromString("[2001:db8::1]:443")
        assertEquals("2001:db8::1", br.getHost())
        assertEquals(443, br.getPort())
    }

    @Test
    fun hostAndPort_bracketAndPortValidation() {
        val nestedBracket = HostAndPort.fromString("[[:]]:108")
        assertEquals("[:]", nestedBracket.getHost())
        assertEquals("[[:]]:108", nestedBracket.toString())
        assertFailsWith<IllegalArgumentException> { HostAndPort.fromString("[hostname]") }
        assertFailsWith<IllegalArgumentException> { HostAndPort.fromString("[::1]suffix") }
        assertFailsWith<IllegalArgumentException> { HostAndPort.fromString("host:\u0ae7") }
        assertFailsWith<IllegalArgumentException> {
            HostAndPort.fromString("host:443").withDefaultPort(-1)
        }
    }

    @Test
    fun mediaType_and_inet() {
        val mt = MediaType.parse("text/html; charset=UTF-8")
        assertEquals("text", mt.type)
        assertEquals("html", mt.subtype)
        assertEquals("UTF-8", mt.parameters["charset"])
        assertTrue(InetAddresses.isInetAddress("127.0.0.1"))
    }
}
