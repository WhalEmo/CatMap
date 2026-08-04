package com.beem.catmap.data.model

import com.beem.catmap.gonderi.ProfilePostCacheData
import com.beem.catmap.gonderi.UserProfileData

data class FullProfileData(
    val profile: UserProfileData,
    val postsCache: ProfilePostCacheData,
    val followerCount: Long,
    val followingCount: Long,
    val isSelfProfile: Boolean,
)