package com.bernaferrari.guavakt.base

import com.bernaferrari.guavakt.annotations.GwtCompatible

@GwtCompatible
abstract class Ticker {
    abstract fun read(): Long

    companion object {
        private val SYSTEM_TICKER = object : Ticker() {
            override fun read(): Long = PlatformClock.nanoTime()
        }

        fun systemTicker(): Ticker = SYSTEM_TICKER
    }
}
