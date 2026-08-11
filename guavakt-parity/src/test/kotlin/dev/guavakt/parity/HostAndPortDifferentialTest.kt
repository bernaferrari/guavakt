package dev.guavakt.parity

import com.google.common.net.HostAndPort as GuavaHostAndPort
import dev.guavakt.net.HostAndPort as GuavaKtHostAndPort
import kotlin.test.Test
import kotlin.test.assertEquals

class HostAndPortDifferentialTest {
    @Test
    fun bracketedAndMalformedInputsMatchGuava() {
        listOf(
            "[2001::1]",
            "[2001::1]:",
            "[2001::1]:443",
            "[[:]]",
            "[[:]]:108",
            "[goo.gl]",
            "[goo.gl]:80",
            "[",
            "[]:",
            "[]:80",
            "[]bad",
            "[::1]suffix",
            "[::1]:+80",
            "[::1]:80 ",
            "[::1]:\u0ae7",
            "example.com:\u0ae7",
            "",
            ":",
            ":123",
            "x:y:z",
            "2001:4860:4864:5",
        ).forEach { input ->
            assertEquals(
                snapshot { GuavaHostAndPort.fromString(input) },
                snapshot { GuavaKtHostAndPort.fromString(input) },
                input,
            )
        }
    }

    @Test
    fun defaultPortValidationAndIdentityMatchGuava() {
        val guavaWithPort = GuavaHostAndPort.fromString("example.com:443")
        val kotlinWithPort = GuavaKtHostAndPort.fromString("example.com:443")
        assertEquals(
            failureName { guavaWithPort.withDefaultPort(-1) },
            failureName { kotlinWithPort.withDefaultPort(-1) },
        )
        assertEquals(
            failureName { GuavaHostAndPort.fromString("example.com").withDefaultPort(65_536) },
            failureName { GuavaKtHostAndPort.fromString("example.com").withDefaultPort(65_536) },
        )

        val guavaDefaulted = GuavaHostAndPort.fromString("[2001:db8::1]").withDefaultPort(443)
        val kotlinDefaulted = GuavaKtHostAndPort.fromString("[2001:db8::1]").withDefaultPort(443)
        assertEquals(guavaDefaulted.toString(), kotlinDefaulted.toString())
        assertEquals(guavaDefaulted.host, kotlinDefaulted.getHost())
        assertEquals(guavaDefaulted.port, kotlinDefaulted.getPort())
    }

    private fun failureName(block: () -> Unit): String? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure::class.simpleName
    }

    private fun snapshot(block: () -> Any): List<Any?> = try {
        when (val value = block()) {
            is GuavaHostAndPort -> listOf(value.host, value.hasPort(), value.getPortOrDefault(-1), value.toString())
            is GuavaKtHostAndPort -> listOf(value.getHost(), value.hasPort(), value.getPortOrDefault(-1), value.toString())
            else -> error("Unexpected value: $value")
        }
    } catch (failure: Throwable) {
        listOf(failure::class.simpleName)
    }
}
