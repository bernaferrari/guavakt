package dev.guavakt.parity

import com.google.common.net.InetAddresses as GuavaInetAddresses
import dev.guavakt.net.InetAddresses as GuavaKtInetAddresses
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class InetAddressesDifferentialTest {
    @Test
    fun literalValidityAndCanonicalRenderingMatchGuava() {
        val invalid = listOf(
            "",
            "016.016.016.016",
            "42.42.42.42.42",
            "42..42.42.42",
            "257.0.0.0",
            "3ffe::1.net",
            "3ffe::1::1",
            "2001::db:::1",
            "6:5:4:3:2:1:0",
            "::7:6:5:4:3:2:1:0",
            "0:1:2:3::4:5:6:7",
            "3ffe::10000",
            "3ffe::-0",
            "3ffe::+0",
            "::1.2.3.4:",
            "2001:db8::1:",
        )
        invalid.forEach { input ->
            assertEquals(GuavaInetAddresses.isInetAddress(input), GuavaKtInetAddresses.isInetAddress(input), input)
        }

        listOf(
            "192.168.0.1",
            "\u0ae7\u0aee\u0ae8.\u0ae7\u0aee\u0aee.\u0ae6.\u0ae7",
            "3ffe::1",
            "\u0ae9ffe::\u0ae7",
            "::7:6:5:4:3:2:1",
            "7:6:5:4:3:2:1::",
            "7::0.128.0.127",
            "2001:0:0:4:0:0:0:8",
            "::1.2.3.4",
        ).forEach { input ->
            assertEquals(GuavaInetAddresses.isInetAddress(input), GuavaKtInetAddresses.isInetAddress(input), input)
            val guava = GuavaInetAddresses.forString(input)
            val kotlin = GuavaKtInetAddresses.forString(input)
            assertContentEquals(guava.address, kotlin, input)
            assertEquals(GuavaInetAddresses.toAddrString(guava), GuavaKtInetAddresses.toAddrString(kotlin), input)
        }
    }

    @Test
    fun uriLiteralFamilyRulesMatchGuava() {
        listOf(
            "192.168.1.1",
            "[3ffe:0:0:0:0:0:0:1]",
            "[::ffff:192.0.2.1]",
            "[192.168.1.1]",
            "3ffe:0:0:0:0:0:0:1",
            "::ffff:192.0.2.1",
            "[3ffe:0:0:0:0:0:0:1",
            "3ffe:0:0:0:0:0:0:1]",
        ).forEach { input ->
            assertEquals(
                GuavaInetAddresses.isUriInetAddress(input),
                GuavaKtInetAddresses.isUriInetAddress(input),
                input,
            )
        }
    }
}
