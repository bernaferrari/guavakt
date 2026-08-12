package com.bernaferrari.guavakt.net

/**
 * Guava InetAddresses — KMP string/byte helpers (no java.net.InetAddress on common).
 * Scoped IPv6 addresses are deliberately rejected because a portable byte value cannot retain a scope.
 */
object InetAddresses {

    fun isInetAddress(ipString: String): Boolean = forStringOrNull(ipString) != null

    fun isIpV4(ipString: String): Boolean = parseIpv4(ipString) != null

    fun isIpV6(ipString: String): Boolean = parseIpv6(ipString) != null

    /** Guava forString — throws on invalid input. */
    fun forString(ipString: String): ByteArray =
        forStringOrNull(ipString) ?: throw IllegalArgumentException("Not an IP string: $ipString")

    fun forStringOrNull(ipString: String): ByteArray? =
        parseIpv4(ipString) ?: parseIpv6(ipString)

    /** Parses an RFC 3986 IP host literal: bare IPv4 or bracketed IPv6. */
    fun forUriString(hostAddr: String): ByteArray {
        val (ipString, expectedByteCount) = if (hostAddr.startsWith('[') && hostAddr.endsWith(']')) {
            hostAddr.substring(1, hostAddr.length - 1) to 16
        } else {
            hostAddr to 4
        }
        val address = forStringOrNull(ipString)
        require(address != null && address.size == expectedByteCount) {
            "Not a valid URI IP literal: $hostAddr"
        }
        return address
    }

    fun forUriStringOrNull(hostAddr: String): ByteArray? = try {
        forUriString(hostAddr)
    } catch (_: IllegalArgumentException) {
        null
    }

    fun isUriInetAddress(ipString: String): Boolean = forUriStringOrNull(ipString) != null

    fun toAddrString(address: ByteArray): String = when (address.size) {
        4 -> address.joinToString(".") { (it.toInt() and 0xff).toString() }
        16 -> formatIpv6(address)
        else -> throw IllegalArgumentException("Not an IP address length: ${address.size}")
    }

    /** Guava toUriString — brackets IPv6. */
    fun toUriString(address: ByteArray): String = when (address.size) {
        4 -> toAddrString(address)
        16 -> "[${toAddrString(address)}]"
        else -> throw IllegalArgumentException("Not an IP address length: ${address.size}")
    }

    fun isMappedIPv4Address(ipString: String): Boolean {
        val bytes = forStringOrNull(ipString) ?: return false
        return isMappedIPv4Address(bytes)
    }

    fun isMappedIPv4Address(address: ByteArray): Boolean {
        if (address.size != 16) return false
        for (i in 0 until 10) if (address[i].toInt() != 0) return false
        return address[10] == 0xff.toByte() && address[11] == 0xff.toByte()
    }

    /** Whether [address] is an IPv4-compatible IPv6 address, excluding `::` and `::1`. */
    fun isCompatIPv4Address(address: ByteArray): Boolean {
        if (address.size != 16 || address.take(12).any { it != 0.toByte() }) return false
        return address[12] != 0.toByte() || address[13] != 0.toByte() ||
            address[14] != 0.toByte() || (address[15] != 0.toByte() && address[15] != 1.toByte())
    }

    /** Returns the final 32 bits of an IPv4-compatible IPv6 [address]. */
    fun getCompatIPv4Address(address: ByteArray): ByteArray {
        require(isCompatIPv4Address(address)) { "Not an IPv4-compatible address: ${toAddrString(address)}" }
        return address.copyOfRange(12, 16)
    }

    /** Whether [address] is in the `2002::/16` 6to4 transition range. */
    fun is6to4Address(address: ByteArray): Boolean =
        address.size == 16 && address[0] == 0x20.toByte() && address[1] == 0x02.toByte()

    /** Returns the relay IPv4 address embedded in a 6to4 IPv6 [address]. */
    fun get6to4IPv4Address(address: ByteArray): ByteArray {
        require(is6to4Address(address)) { "Not a 6to4 address: ${toAddrString(address)}" }
        return address.copyOfRange(2, 6)
    }

    /** Immutable decoded portions of a Teredo address, represented portably as IPv4 byte arrays. */
    class TeredoInfo(server: ByteArray?, client: ByteArray?, private val port: Int, private val flags: Int) {
        private val server = ipv4OrAny(server)
        private val client = ipv4OrAny(client)

        init {
            require(port in 0..0xffff) { "port '$port' is out of range (0 <= port <= 65535)" }
            require(flags in 0..0xffff) { "flags '$flags' is out of range (0 <= flags <= 65535)" }
        }

        fun getServer(): ByteArray = server.copyOf()
        fun getClient(): ByteArray = client.copyOf()
        fun getPort(): Int = port
        fun getFlags(): Int = flags

        private companion object {
            fun ipv4OrAny(value: ByteArray?): ByteArray {
                if (value == null) return ByteArray(4)
                require(value.size == 4) { "Expected an IPv4 address" }
                return value.copyOf()
            }
        }
    }

    /** Whether [address] is in the `2001:0000::/32` Teredo transition range. */
    fun isTeredoAddress(address: ByteArray): Boolean =
        address.size == 16 && address[0] == 0x20.toByte() && address[1] == 0x01.toByte() &&
            address[2] == 0.toByte() && address[3] == 0.toByte()

    /** Decodes server, client, port, and flags from a Teredo IPv6 [address]. */
    fun getTeredoInfo(address: ByteArray): TeredoInfo {
        require(isTeredoAddress(address)) { "Not a Teredo address: ${toAddrString(address)}" }
        val server = address.copyOfRange(4, 8)
        val flags = unsignedShort(address, 8)
        val port = unsignedShort(address, 10).inv() and 0xffff
        val client = ByteArray(4) { index -> address[12 + index].toInt().inv().toByte() }
        return TeredoInfo(server, client, port, flags)
    }

    /** Whether [address] has the ISATAP interface-identifier marker. */
    fun isIsatapAddress(address: ByteArray): Boolean {
        if (address.size != 16 || isTeredoAddress(address)) return false
        val interfaceFirstByte = address[8].toInt() and 0xff
        return (interfaceFirstByte or 0x03) == 0x03 &&
            address[9] == 0.toByte() && address[10] == 0x5e.toByte() && address[11] == 0xfe.toByte()
    }

    /** Returns the final IPv4 address embedded in an ISATAP IPv6 [address]. */
    fun getIsatapIPv4Address(address: ByteArray): ByteArray {
        require(isIsatapAddress(address)) { "Not an ISATAP address: ${toAddrString(address)}" }
        return address.copyOfRange(12, 16)
    }

    /** Whether [address] is a Guava-recognized IPv6 transition address with an IPv4 client. */
    fun hasEmbeddedIPv4ClientAddress(address: ByteArray): Boolean =
        isCompatIPv4Address(address) || is6to4Address(address) || isTeredoAddress(address)

    /**
     * Returns the IPv4 client embedded in a compatible, 6to4, or Teredo IPv6 [address].
     *
     * IPv4-mapped addresses deliberately do not qualify: Guava treats them as an OS/JVM address
     * representation detail rather than one of these client-address transition mechanisms.
     */
    fun getEmbeddedIPv4ClientAddress(address: ByteArray): ByteArray = when {
        isCompatIPv4Address(address) -> getCompatIPv4Address(address)
        is6to4Address(address) -> get6to4IPv4Address(address)
        isTeredoAddress(address) -> getTeredoInfo(address).getClient()
        else -> throw IllegalArgumentException("${toAddrString(address)} has no embedded IPv4 address")
    }

    /**
     * Coerces [address] to an IPv4 address using Guava's fixed Murmur3-32 mapping for IPv6.
     *
     * IPv4 input is copied unchanged; `::` and `::1` map to all-zero and loopback IPv4. Other
     * IPv6 values map into `224.0.0.0/3`, preserving Guava's stable indexing behavior.
     */
    fun getCoercedIPv4Address(address: ByteArray): ByteArray {
        requireAddressLength(address)
        if (address.size == 4) return address.copyOf()
        // java.net.InetAddress collapses mapped IPv6 input to Inet4Address before Guava sees it.
        // The portable byte representation retains all 16 bytes, so apply that normalization here.
        if (isMappedIPv4Address(address)) return address.copyOfRange(12, 16)
        val leadingBytesAreZero = address.take(15).all { it == 0.toByte() }
        if (leadingBytesAreZero && address[15] == 1.toByte()) return byteArrayOf(127, 0, 0, 1)
        if (leadingBytesAreZero && address[15] == 0.toByte()) return ByteArray(4)

        val addressAsLong = if (hasEmbeddedIPv4ClientAddress(address)) {
            ipv4ToInteger(getEmbeddedIPv4ClientAddress(address)).toLong()
        } else {
            bigEndianLong(address, 0)
        }
        var coerced = murmur3_32HashLong(addressAsLong) or 0xe0000000.toInt()
        if (coerced == -1) coerced = -2
        return fromInteger(coerced)
    }

    /** Coerces any IPv4 or IPv6 [address] to a network-byte-order signed integer. */
    fun coerceToInteger(address: ByteArray): Int = ipv4ToInteger(getCoercedIPv4Address(address))

    fun fromInteger(address: Int): ByteArray = byteArrayOf(
        (address ushr 24).toByte(),
        (address ushr 16).toByte(),
        (address ushr 8).toByte(),
        address.toByte(),
    )

    /** Returns the next IPv4 or IPv6 address; throws rather than wrapping at the maximum. */
    fun increment(address: ByteArray): ByteArray {
        requireAddressLength(address)
        val out = address.copyOf()
        var index = out.lastIndex
        while (index >= 0 && out[index] == 0xff.toByte()) out[index--] = 0
        require(index >= 0) { "Incrementing ${toAddrString(address)} would wrap." }
        out[index] = (out[index].toInt() + 1).toByte()
        return out
    }

    /** Returns the preceding IPv4 or IPv6 address; throws rather than wrapping at all-zero. */
    fun decrement(address: ByteArray): ByteArray {
        requireAddressLength(address)
        val out = address.copyOf()
        var index = out.lastIndex
        while (index >= 0 && out[index] == 0.toByte()) out[index--] = 0xff.toByte()
        require(index >= 0) { "Decrementing ${toAddrString(address)} would wrap." }
        out[index] = (out[index].toInt() - 1).toByte()
        return out
    }

    /** Returns whether [address] is the greatest address in its IPv4 or IPv6 family. */
    fun isMaximum(address: ByteArray): Boolean {
        requireAddressLength(address)
        return address.all { it == 0xff.toByte() }
    }

    /** Reverses a four- or sixteen-byte little-endian address into normal network byte order. */
    fun fromLittleEndianByteArray(address: ByteArray): ByteArray {
        requireAddressLength(address)
        return address.reversedArray()
    }

    private fun parseIpv4(ipString: String): ByteArray? {
        val parts = ipString.split('.')
        if (parts.size != 4) return null
        val out = ByteArray(4)
        for (i in 0..3) {
            val p = parts[i]
            if (p.length !in 1..3 || (p.length > 1 && p[0] == '0') || !p.all { it.isDigit() }) return null
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            out[i] = n.toByte()
        }
        return out
    }

    private fun parseIpv6(ipString: String): ByteArray? {
        if (ipString.isEmpty() || ipString.count { it == ':' } < 2) return null
        // IPv4-mapped / embedded IPv4 only allowed as the final component
        var s = ipString
        var embeddedV4: ByteArray? = null
        val lastColon = s.lastIndexOf(':')
        if (lastColon >= 0 && s.indexOf('.', lastColon) >= 0) {
            val v4Part = s.substring(lastColon + 1)
            embeddedV4 = parseIpv4(v4Part) ?: return null
            // Represent embedded IPv4 as two hextets so word accounting matches Guava
            val hi = ((embeddedV4[0].toInt() and 0xff) shl 8) or (embeddedV4[1].toInt() and 0xff)
            val lo = ((embeddedV4[2].toInt() and 0xff) shl 8) or (embeddedV4[3].toInt() and 0xff)
            s = s.substring(0, lastColon + 1) + hi.toString(16) + ":" + lo.toString(16)
        }
        val compressed = s.contains("::")
        if (compressed && s.indexOf("::") != s.lastIndexOf("::")) return null
        val halves = if (compressed) s.split("::", limit = 2) else listOf(s)
        fun parseHalf(half: String): List<Int>? {
            if (half.isEmpty()) return emptyList()
            return half.split(':').map { part ->
                val v = parseHextet(part) ?: return null
                v
            }
        }
        val left = parseHalf(halves[0]) ?: return null
        val right = if (halves.size > 1) parseHalf(halves[1]) ?: return null else emptyList()
        val total = left.size + right.size
        if (compressed) {
            // The compression marker must replace at least one 16-bit word.
            if (total >= 8) return null
        } else if (total != 8) return null
        val words = IntArray(8)
        for (i in left.indices) words[i] = left[i]
        for (i in right.indices) words[8 - right.size + i] = right[i]
        val out = ByteArray(16)
        for (i in 0 until 8) {
            out[i * 2] = (words[i] ushr 8).toByte()
            out[i * 2 + 1] = words[i].toByte()
        }
        return out
    }

    private fun parseHextet(hextet: String): Int? {
        if (hextet.length !in 1..4) return null
        if (!hextet.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) return null
        return hextet.toIntOrNull(16)?.takeIf { it in 0..0xffff }
    }

    private fun formatIpv6(address: ByteArray): String {
        val words = IntArray(8) { i ->
            ((address[i * 2].toInt() and 0xff) shl 8) or (address[i * 2 + 1].toInt() and 0xff)
        }
        // find longest zero run for compression
        var bestStart = -1
        var bestLen = 0
        var i = 0
        while (i < 8) {
            if (words[i] == 0) {
                val start = i
                while (i < 8 && words[i] == 0) i++
                val len = i - start
                if (len > bestLen) {
                    bestStart = start
                    bestLen = len
                }
            } else i++
        }
        if (bestLen < 2) {
            return words.joinToString(":") { it.toString(16) }
        }
        val left = (0 until bestStart).joinToString(":") { words[it].toString(16) }
        val rightStart = bestStart + bestLen
        val right = (rightStart until 8).joinToString(":") { words[it].toString(16) }
        return left + "::" + right
    }

    private fun requireAddressLength(address: ByteArray) {
        require(address.size == 4 || address.size == 16) { "Not an IP address length: ${address.size}" }
    }

    private fun ipv4ToInteger(address: ByteArray): Int {
        require(address.size == 4) { "Expected an IPv4 address" }
        return ((address[0].toInt() and 0xff) shl 24) or
            ((address[1].toInt() and 0xff) shl 16) or
            ((address[2].toInt() and 0xff) shl 8) or
            (address[3].toInt() and 0xff)
    }

    private fun unsignedShort(address: ByteArray, offset: Int): Int =
        ((address[offset].toInt() and 0xff) shl 8) or (address[offset + 1].toInt() and 0xff)

    private fun bigEndianLong(address: ByteArray, offset: Int): Long {
        var result = 0L
        for (index in offset until offset + Long.SIZE_BYTES) {
            result = (result shl Byte.SIZE_BITS) or (address[index].toLong() and 0xffL)
        }
        return result
    }

    /** Local fixed Murmur3-32 for Guava's InetAddresses coercion without coupling net to hash. */
    private fun murmur3_32HashLong(input: Long): Int {
        var h1 = mixH1(0, mixK1(input.toInt()))
        h1 = mixH1(h1, mixK1((input ushr Int.SIZE_BITS).toInt()))
        return fmix(h1, Long.SIZE_BYTES)
    }

    private fun mixK1(input: Int): Int {
        var result = input * -0x3361d2af
        result = (result shl 15) or (result ushr 17)
        return result * 0x1b873593
    }

    private fun mixH1(input: Int, k1: Int): Int {
        var result = input xor k1
        result = (result shl 13) or (result ushr 19)
        return result * 5 + 0xe6546b64.toInt()
    }

    private fun fmix(input: Int, length: Int): Int {
        var result = input xor length
        result = result xor (result ushr 16)
        result *= 0x85ebca6b.toInt()
        result = result xor (result ushr 13)
        result *= 0xc2b2ae35.toInt()
        return result xor (result ushr 16)
    }
}
