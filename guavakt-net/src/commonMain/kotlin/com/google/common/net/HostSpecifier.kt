package dev.guavakt.net

/**
 * Guava HostSpecifier — validates host strings (domain or IP literal).
 */
class HostSpecifier private constructor(private val canonicalForm: String) {
    override fun toString(): String = canonicalForm
    override fun equals(other: Any?): Boolean = other is HostSpecifier && canonicalForm == other.canonicalForm
    override fun hashCode(): Int = canonicalForm.hashCode()

    companion object {
        fun fromValid(specifier: String): HostSpecifier {
            val parsed = HostAndPort.fromString(specifier)
            require(!parsed.hasPort()) { "Host specifier must not include a port: $specifier" }
            val host = parsed.getHost()
            val address = InetAddresses.forStringOrNull(host)
            if (address != null) return HostSpecifier(InetAddresses.toUriString(address))
            val domain = InternetDomainName.from(host)
            require(domain.hasPublicSuffix()) { "Domain name does not have a recognized public suffix: $host" }
            return HostSpecifier(domain.toString())
        }

        fun from(specifier: String): HostSpecifier = try {
            fromValid(specifier)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid host specifier: $specifier", e)
        }

        fun isValid(specifier: String): Boolean = try {
            fromValid(specifier); true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
