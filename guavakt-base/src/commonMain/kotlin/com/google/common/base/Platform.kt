package dev.guavakt.base

internal object Platform {
    fun precomputeCharMatcher(matcher: CharMatcher): CharMatcher = matcher
    fun getPatternCompiler(): PatternCompiler = object : PatternCompiler {
        override fun compile(pattern: String): CommonPattern = CommonPattern.compile(pattern)
        override fun isPcreLike(): Boolean = true
    }
    fun formatCompact4Digits(value: Double): String = value.toString()
    fun stringIsNullOrEmpty(string: String?): Boolean = string.isNullOrEmpty()
}
