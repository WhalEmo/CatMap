package com.beem.catmap.ui.profile_v2.myprofile

import com.beem.catmap.data.model.Post
import com.beem.catmap.data.model.UserProfileData

data class MyProfileUiState(
    val isLoading: Boolean = false,
    val user: UserProfileData? = null,
    val posts: List<Post> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isLastPage: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)