package com.bernaferrari.guavakt.net

import com.bernaferrari.guavakt.base.Preconditions

class HostAndPort private constructor(
    private val host: String,
    private val port: Int,
    private val hasBracketlessColons: Boolean,
) {
    fun getHost(): String = host
    fun hasPort(): Boolean = port >= 0
    fun getPort(): Int {
        Preconditions.checkState(hasPort())
        return port
    }
    fun getPortOrDefault(defaultPort: Int): Int = if (hasPort()) port else defaultPort
    fun withDefaultPort(defaultPort: Int): HostAndPort {
        Preconditions.checkArgument(isValidPort(defaultPort), "Port out of range: %s", defaultPort)
        return if (hasPort()) this else HostAndPort(host, defaultPort, hasBracketlessColons)
    }
    fun requireBracketsForIPv6(): HostAndPort {
        Preconditions.checkArgument(!hasBracketlessColons, "Possible bracketless IPv6 literal: %s", host)
        return this
    }
    override fun toString(): String = buildString {
        if (host.indexOf(':') >= 0) append('[').append(host).append(']') else append(host)
        if (hasPort()) append(':').append(port)
    }
    override fun equals(other: Any?): Boolean =
        other is HostAndPort && host == other.host && port == other.port
    override fun hashCode(): Int = host.hashCode() * 31 + port
    companion object {
        fun fromHost(host: String): HostAndPort {
            val parsed = fromString(host)
            Preconditions.checkArgument(!parsed.hasPort(), "Host has a port: %s", host)
            return parsed
        }
        fun fromParts(host: String, port: Int): HostAndPort {
            Preconditions.checkArgument(isValidPort(port), "Port out of range: %s", port)
            val parsed = fromString(host)
            Preconditions.checkArgument(!parsed.hasPort(), "Host has a port: %s", host)
            return HostAndPort(parsed.host, port, parsed.hasBracketlessColons)
        }
        fun fromString(hostPortString: String): HostAndPort {
            Preconditions.checkNotNull(hostPortString)
            val host: String
            var portString: String? = null
            var hasBracketlessColons = false
            if (hostPortString.startsWith("[")) {
                val colon = hostPortString.indexOf(':')
                val close = hostPortString.lastIndexOf(']')
                Preconditions.checkArgument(
                    colon != -1 && close > colon,
                    "Invalid bracketed host/port: %s",
                    hostPortString,
                )
                host = hostPortString.substring(1, close)
                if (close + 1 < hostPortString.length) {
                    Preconditions.checkArgument(
                        hostPortString[close + 1] == ':',
                        "Only a colon may follow a close bracket: %s",
                        hostPortString,
                    )
                    portString = hostPortString.substring(close + 2)
                    Preconditions.checkArgument(
                        portString.all { it in '0'..'9' },
                        "Port must be numeric: %s",
                        hostPortString,
                    )
                }
            } else {
                val colon = hostPortString.indexOf(':')
                if (colon >= 0 && hostPortString.indexOf(':', colon + 1) == -1) {
                    host = hostPortString.substring(0, colon)
                    portString = hostPortString.substring(colon + 1)
                } else {
                    host = hostPortString
                    hasBracketlessColons = colon >= 0
                }
            }
            var port = -1
            if (!portString.isNullOrEmpty()) {
                port = parseAsciiPort(portString)
                    ?: throw IllegalArgumentException("Unparseable port number: $hostPortString")
                Preconditions.checkArgument(isValidPort(port), "Port number out of range: %s", hostPortString)
            }
            return HostAndPort(host, port, hasBracketlessColons)
        }

        private fun parseAsciiPort(port: String): Int? =
            if (port.all { it in '0'..'9' }) port.toIntOrNull() else null

        private fun isValidPort(port: Int): Boolean = port in 0..65535
    }
}
