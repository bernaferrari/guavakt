package com.bernaferrari.guavakt.thirdparty.publicsuffix

/** Guava PublicSuffixType — PRIVATE vs REGISTRY public suffix nodes (PSL). */
enum class PublicSuffixType(private val innerNodeCode: Char, private val leafNodeCode: Char) {
    PRIVATE('!', '?'),
    REGISTRY('+', '*');
    fun getInnerNodeCode(): Char = innerNodeCode
    fun getLeafNodeCode(): Char = leafNodeCode
    companion object {
        fun fromCode(code: Char): PublicSuffixType = when (code) {
            '!', '?' -> PRIVATE
            '+', '*' -> REGISTRY
            else -> throw IllegalArgumentException("No enum corresponding to given code: $code")
        }
    }
}
