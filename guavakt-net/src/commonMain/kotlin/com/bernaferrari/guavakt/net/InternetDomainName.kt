package com.bernaferrari.guavakt.net

import com.bernaferrari.guavakt.base.Preconditions
import com.bernaferrari.guavakt.thirdparty.publicsuffix.PublicSuffixPatterns

/**
 * Guava InternetDomainName — public-suffix aware domains using full Guava PSL trie.
 */
class InternetDomainName private constructor(
    private val name: String,
    private val parts: List<String>,
) {
    override fun toString(): String = name
    fun parts(): List<String> = parts
    fun hasParent(): Boolean = parts.size > 1
    fun parent(): InternetDomainName {
        Preconditions.checkState(hasParent())
        return from(parts.drop(1).joinToString("."))
    }

    fun publicSuffix(): String? = PublicSuffixPatterns.TRIE.getPublicSuffix(name)

    fun hasPublicSuffix(): Boolean = publicSuffix() != null
    fun isPublicSuffix(): Boolean = publicSuffix() == name
    fun isUnderPublicSuffix(): Boolean {
        val ps = publicSuffix() ?: return false
        return name != ps && name.endsWith(".$ps")
    }
    fun isTopPrivateDomain(): Boolean {
        val ps = publicSuffix() ?: return false
        return parts.size == ps.split('.').size + 1
    }
    fun topPrivateDomain(): InternetDomainName {
        Preconditions.checkState(hasPublicSuffix())
        val ps = publicSuffix()!!
        val psParts = ps.split('.')
        val tpdParts = parts.takeLast(psParts.size + 1)
        return from(tpdParts.joinToString("."))
    }

    /** Registry (ICANN) public suffix only — excludes PRIVATE PSL nodes (e.g. github.io). */
    fun registrySuffix(): String? = PublicSuffixPatterns.TRIE.getRegistrySuffix(name)

    fun hasRegistrySuffix(): Boolean = registrySuffix() != null

    fun isRegistrySuffix(): Boolean = registrySuffix() == name

    fun isUnderRegistrySuffix(): Boolean {
        val rs = registrySuffix() ?: return false
        return name != rs && name.endsWith(".$rs")
    }

    fun topDomainUnderRegistrySuffix(): InternetDomainName {
        Preconditions.checkState(hasRegistrySuffix())
        val rs = registrySuffix()!!
        val rsParts = rs.split('.')
        val topParts = parts.takeLast(rsParts.size + 1)
        return from(topParts.joinToString("."))
    }

    fun child(leftParts: String): InternetDomainName = from("$leftParts.$name")

    companion object {
        fun from(domain: String): InternetDomainName {
            val name = Preconditions.checkNotNull(domain).trim().lowercase().trimEnd('.')
            Preconditions.checkArgument(name.isNotEmpty()) { "domain must not be empty" }
            Preconditions.checkArgument(name.length <= 253) { "domain name too long: $domain" }
            Preconditions.checkArgument(!name.startsWith('.')) { "domain must not start with '.'" }
            Preconditions.checkArgument(!name.contains("..")) { "domain must not contain empty labels" }
            val parts = name.split('.')
            Preconditions.checkArgument(parts.isNotEmpty() && parts.all { it.isNotEmpty() }) {
                "domain has empty labels: $domain"
            }
            for (p in parts) {
                Preconditions.checkArgument(p.length <= 63) { "label too long: $p" }
                Preconditions.checkArgument(p.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }) {
                    "illegal label characters: $p"
                }
                Preconditions.checkArgument(!p.startsWith('-') && !p.endsWith('-')) {
                    "label must not start/end with '-': $p"
                }
            }
            return InternetDomainName(name, parts)
        }
        fun isValid(name: String): Boolean = try {
            from(name); true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
