package com.beem.catmap.gonderi

import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem

data class ProfilePostCacheData(
    val posts: List<Gonderi>,
    val idList: List<GonderilenKediItem>,
    val offset: Int,
    val isLastPage: Boolean
)