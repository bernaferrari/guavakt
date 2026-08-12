package com.bernaferrari.guavakt.net

import com.bernaferrari.guavakt.escape.Escaper

object UrlEscapers {
    private val PATH_ESCAPER = PercentEscaper("-._~!\$'()*,;&=@:+", false)
    private val FORM_ESCAPER = PercentEscaper("-._*@", true)
    private val FRAGMENT_ESCAPER = PercentEscaper("-._~!\$'()*,;&=@:+/?", false)

    fun urlPathSegmentEscaper(): Escaper = PATH_ESCAPER
    fun urlFormParameterEscaper(): Escaper = FORM_ESCAPER
    fun urlFragmentEscaper(): Escaper = FRAGMENT_ESCAPER
}
