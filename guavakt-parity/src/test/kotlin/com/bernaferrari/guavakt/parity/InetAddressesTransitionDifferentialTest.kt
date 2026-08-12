package com.bernaferrari.guavakt.parity

import com.google.common.net.InetAddresses as GuavaInetAddresses
import com.bernaferrari.guavakt.net.InetAddresses as GuavaKtInetAddresses
import java.net.Inet6Address
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class InetAddressesTransitionDifferentialTest {
    @Test
    fun transitionAddressRecognitionAndExtractionMatchGuava() {
        listOf("::", "::1", "3ffe::1", "::1.2.3.4", "::102:304").forEach { input ->
            val guava = v6(input)
            val kotlin = bytes(input)
            assertEquals(GuavaInetAddresses.isCompatIPv4Address(guava), GuavaKtInetAddresses.isCompatIPv4Address(kotlin), input)
            assertEquals(
                failureName { GuavaInetAddresses.getCompatIPv4Address(guava) },
                failureName { GuavaKtInetAddresses.getCompatIPv4Address(kotlin) },
                input,
            )
        }
        listOf("::1.2.3.4", "::102:304").forEach { input ->
            assertContentEquals(
                GuavaInetAddresses.getCompatIPv4Address(v6(input)).address,
                GuavaKtInetAddresses.getCompatIPv4Address(bytes(input)),
                input,
            )
        }

        listOf("::1.2.3.4", "3ffe::1", "::", "::1", "2002:0102:0304::1").forEach { input ->
            assertEquals(
                GuavaInetAddresses.is6to4Address(v6(input)),
                GuavaKtInetAddresses.is6to4Address(bytes(input)),
                input,
            )
        }
        assertContentEquals(
            GuavaInetAddresses.get6to4IPv4Address(v6("2002:0102:0304::1")).address,
            GuavaKtInetAddresses.get6to4IPv4Address(bytes("2002:0102:0304::1")),
        )

        val teredo = "2001:0000:4136:e378:8000:63bf:3fff:fdd2"
        val guavaTeredo = GuavaInetAddresses.getTeredoInfo(v6(teredo))
        val kotlinTeredo = GuavaKtInetAddresses.getTeredoInfo(bytes(teredo))
        assertEquals(GuavaInetAddresses.isTeredoAddress(v6(teredo)), GuavaKtInetAddresses.isTeredoAddress(bytes(teredo)))
        assertContentEquals(guavaTeredo.server.address, kotlinTeredo.getServer())
        assertContentEquals(guavaTeredo.client.address, kotlinTeredo.getClient())
        assertEquals(guavaTeredo.port, kotlinTeredo.getPort())
        assertEquals(guavaTeredo.flags, kotlinTeredo.getFlags())

        listOf(
            "2001:db8::5efe:102:304",
            "2001:db8::100:5efe:102:304",
            "2001:db8::200:5efe:102:304",
            "2001:db8::300:5efe:102:304",
            "::1.2.3.4",
            "3ffe::1",
            "2001:db8::0040:5efe:102:304",
            "2001:0:102:203:200:5efe:506:708",
        ).forEach { input ->
            assertEquals(
                GuavaInetAddresses.isIsatapAddress(v6(input)),
                GuavaKtInetAddresses.isIsatapAddress(bytes(input)),
                input,
            )
        }
        assertContentEquals(
            GuavaInetAddresses.getIsatapIPv4Address(v6("2001:db8::5efe:102:304")).address,
            GuavaKtInetAddresses.getIsatapIPv4Address(bytes("2001:db8::5efe:102:304")),
        )
    }

    @Test
    fun embeddedCoercionAndAddressBoundariesMatchGuava() {
        listOf(
            "127.0.0.1",
            "::",
            "::1",
            "::1.2.3.4",
            "::ffff:192.0.2.1",
            "2002:0102:0304::1",
            "2001:0000:4136:e378:8000:63bf:3fff:fdd2",
            "2001:4860::1",
        ).forEach { input ->
            val guava = GuavaInetAddresses.forString(input)
            val kotlin = bytes(input)
            val guavaHasEmbedded = (guava as? Inet6Address)?.let(GuavaInetAddresses::hasEmbeddedIPv4ClientAddress) ?: false
            assertEquals(
                guavaHasEmbedded,
                GuavaKtInetAddresses.hasEmbeddedIPv4ClientAddress(kotlin),
                input,
            )
            assertContentEquals(
                GuavaInetAddresses.getCoercedIPv4Address(guava).address,
                GuavaKtInetAddresses.getCoercedIPv4Address(kotlin),
                input,
            )
            assertEquals(
                GuavaInetAddresses.coerceToInteger(guava),
                GuavaKtInetAddresses.coerceToInteger(kotlin),
                input,
            )
        }

        listOf("0.0.0.0", "127.0.0.1", "255.255.255.254", "255.255.255.255", "::", "::1", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffe", "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff").forEach { input ->
            val guava = GuavaInetAddresses.forString(input)
            val kotlin = bytes(input)
            assertEquals(GuavaInetAddresses.isMaximum(guava), GuavaKtInetAddresses.isMaximum(kotlin), input)
            assertEquals(
                incrementSnapshot { GuavaInetAddresses.increment(guava) },
                incrementSnapshot { GuavaKtInetAddresses.increment(kotlin) },
                input,
            )
            assertEquals(
                incrementSnapshot { GuavaInetAddresses.decrement(guava) },
                incrementSnapshot { GuavaKtInetAddresses.decrement(kotlin) },
                input,
            )
        }

        listOf(byteArrayOf(1, 2, 3, 4), ByteArray(16) { it.toByte() }).forEach { littleEndian ->
            assertContentEquals(
                GuavaInetAddresses.fromLittleEndianByteArray(littleEndian).address,
                GuavaKtInetAddresses.fromLittleEndianByteArray(littleEndian),
            )
        }
    }

    private fun v6(input: String): Inet6Address = GuavaInetAddresses.forString(input) as Inet6Address
    private fun bytes(input: String): ByteArray = GuavaKtInetAddresses.forString(input)

    private fun failureName(block: () -> Any): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

    private fun incrementSnapshot(block: () -> Any): String = try {
        when (val value = block()) {
            is ByteArray -> GuavaKtInetAddresses.toAddrString(value)
            is java.net.InetAddress -> GuavaInetAddresses.toAddrString(value)
            else -> error("Unexpected address: $value")
        }
    } catch (failure: Throwable) {
        failure::class.simpleName ?: failure::class.toString()
    }
}
