package com.bernaferrari.guavakt.cache

internal inline fun <T> monitorSync(lock: Any, block: () -> T): T = platformCacheSync(lock, block)

internal expect inline fun <T> platformCacheSync(lock: Any, block: () -> T): T
