package com.beem.catmap.utils.cache

data class CacheEntry<T>(
    val data: T,
    val timestamp: Long
)