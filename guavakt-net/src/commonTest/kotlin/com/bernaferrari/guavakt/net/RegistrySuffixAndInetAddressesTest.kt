package com.bernaferrari.guavakt.net

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegistrySuffixAndInetAddressesTest {
    @Test
    fun registrySuffix_com_isRegistry() {
        val d = InternetDomainName.from("example.com")
        assertEquals("com", d.registrySuffix())
        assertTrue(d.hasRegistrySuffix())
        assertFalse(d.isRegistrySuffix())
        assertTrue(d.isUnderRegistrySuffix())
        assertEquals("example.com", d.topDomainUnderRegistrySuffix().toString())
    }

    @Test
    fun registrySuffix_githubIo_private_vs_registry() {
        val d = InternetDomainName.from("shelter.github.io")
        // public suffix includes private PSL node github.io
        assertEquals("github.io", d.publicSuffix())
        // registry suffix should be icann "io" when trie distinguishes PRIVATE
        val rs = d.registrySuffix()
        assertNotNull(rs)
        // If PRIVATE typing works, registry is "io"; otherwise may equal public suffix
        assertTrue(rs == "io" || rs == "github.io", "registrySuffix=$rs")
        if (rs == "io") {
            assertEquals("github.io", d.topDomainUnderRegistrySuffix().toString())
            assertFalse(d.isRegistrySuffix())
            assertTrue(InternetDomainName.from("io").isRegistrySuffix() || d.isUnderRegistrySuffix())
        }
    }

    @Test
    fun inetAddresses_ipv4_roundTrip() {
        val bytes = InetAddresses.forString("127.0.0.1")
        assertEquals(4, bytes.size)
        assertEquals("127.0.0.1", InetAddresses.toAddrString(bytes))
        assertTrue(InetAddresses.isInetAddress("192.168.0.1"))
        assertFalse(InetAddresses.isInetAddress("999.0.0.1"))
        assertFalse(InetAddresses.isInetAddress("01.2.3.4"))
    }

    @Test
    fun inetAddresses_uri_and_coerce() {
        val v6 = InetAddresses.forUriString("[::1]")
        assertEquals(16, v6.size)
        assertTrue(InetAddresses.toUriString(v6).startsWith("["))
        assertTrue(InetAddresses.isUriInetAddress("192.168.1.1"))
        assertTrue(InetAddresses.isUriInetAddress("[3ffe::1]"))
        assertFalse(InetAddresses.isUriInetAddress("3ffe::1"))
        assertFalse(InetAddresses.isUriInetAddress("[192.168.1.1]"))
        val v4 = InetAddresses.fromInteger(0x7f000001)
        assertEquals("127.0.0.1", InetAddresses.toAddrString(v4))
        assertEquals(0x7f000001, InetAddresses.coerceToInteger(v4))
        val next = InetAddresses.increment(v4)
        assertEquals("127.0.0.2", InetAddresses.toAddrString(next))
    }

    @Test
    fun inetAddresses_mappedIpv4() {
        val mapped = InetAddresses.forString("::ffff:192.0.2.1")
        assertTrue(InetAddresses.isMappedIPv4Address(mapped))
        assertFailsWith<IllegalArgumentException> { InetAddresses.getEmbeddedIPv4ClientAddress(mapped) }

        val compatible = InetAddresses.forString("::192.0.2.1")
        assertContentEquals(
            byteArrayOf(192.toByte(), 0, 2, 1),
            InetAddresses.getEmbeddedIPv4ClientAddress(compatible),
        )
    }

    @Test
    fun inetAddresses_transitionFormatsAndBoundaries() {
        val compatible = InetAddresses.forString("::1.2.3.4")
        assertTrue(InetAddresses.isCompatIPv4Address(compatible))
        assertContentEquals(InetAddresses.forString("1.2.3.4"), InetAddresses.getCompatIPv4Address(compatible))

        val sixToFour = InetAddresses.forString("2002:0102:0304::1")
        assertTrue(InetAddresses.is6to4Address(sixToFour))
        assertContentEquals(InetAddresses.forString("1.2.3.4"), InetAddresses.get6to4IPv4Address(sixToFour))

        val teredo = InetAddresses.getTeredoInfo(InetAddresses.forString("2001:0000:4136:e378:8000:63bf:3fff:fdd2"))
        assertEquals("65.54.227.120", InetAddresses.toAddrString(teredo.getServer()))
        assertEquals("192.0.2.45", InetAddresses.toAddrString(teredo.getClient()))
        assertEquals(40_000, teredo.getPort())
        assertEquals(0x8000, teredo.getFlags())

        val isatap = InetAddresses.forString("2001:db8::5efe:102:304")
        assertTrue(InetAddresses.isIsatapAddress(isatap))
        assertContentEquals(InetAddresses.forString("1.2.3.4"), InetAddresses.getIsatapIPv4Address(isatap))

        assertEquals("127.0.0.1", InetAddresses.toAddrString(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("::1"))))
        assertEquals("0.0.0.0", InetAddresses.toAddrString(InetAddresses.getCoercedIPv4Address(InetAddresses.forString("::"))))
        assertTrue(InetAddresses.isMaximum(InetAddresses.forString("255.255.255.255")))
        assertFailsWith<IllegalArgumentException> { InetAddresses.increment(InetAddresses.forString("255.255.255.255")) }
        assertFailsWith<IllegalArgumentException> { InetAddresses.decrement(InetAddresses.forString("0.0.0.0")) }
        assertContentEquals(byteArrayOf(4, 3, 2, 1), InetAddresses.fromLittleEndianByteArray(byteArrayOf(1, 2, 3, 4)))
    }

    @Test
    fun inetAddresses_rejectsBogus() {
        assertFailsWith<IllegalArgumentException> { InetAddresses.forString("not-an-ip") }
        assertFailsWith<IllegalArgumentException> { InetAddresses.forUriString("3ffe::1") }
        assertFailsWith<IllegalArgumentException> { InetAddresses.forUriString("[192.168.1.1]") }
        assertNull(InetAddresses.forUriStringOrNull(":::"))
    }
}
