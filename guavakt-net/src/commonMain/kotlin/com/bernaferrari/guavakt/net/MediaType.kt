package com.bernaferrari.guavakt.net

object MediaType {
    data class Parsed(val type: String, val subtype: String, val parameters: Map<String, String>) {
        override fun toString(): String = buildString {
            append(type).append('/').append(subtype)
            for ((k, v) in parameters) append("; ").append(k).append('=').append(v)
        }
    }
    fun parse(input: String): Parsed {
        val main = input.substringBefore(';').trim()
        val slash = main.indexOf('/')
        require(slash > 0) { "no subtype: $input" }
        val type = main.substring(0, slash).lowercase()
        val subtype = main.substring(slash + 1).lowercase()
        val params = LinkedHashMap<String, String>()
        val rest = input.substringAfter(';', missingDelimiterValue = "")
        if (rest.isNotEmpty()) {
            for (part in rest.split(';')) {
                val eq = part.indexOf('=')
                if (eq > 0) {
                    params[part.substring(0, eq).trim().lowercase()] =
                        part.substring(eq + 1).trim().trim('"')
                }
            }
        }
        return Parsed(type, subtype, params)
    }
}
