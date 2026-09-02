package com.beem.catmap.ui.profile_v2.otherprofile

import com.beem.catmap.data.model.Post
import com.beem.catmap.data.model.UserProfileData

data class OtherProfileUiState(
    val isLoading: Boolean = true,
    val user: UserProfileData? = null,
    val followStatus: OtherFollowStatus = OtherFollowStatus.LOADING,
    val isBlockedByMe: Boolean = false,
    val isBlockedByThem: Boolean = false,
    val isMyFollower: Boolean = false,
    val isActionLoading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isLastPage: Boolean = false,
    val isAccessDenied: Boolean = false,
    val errorMessage: String? = null
)