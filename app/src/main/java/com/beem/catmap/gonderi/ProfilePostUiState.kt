package com.beem.catmap.gonderi

import com.beem.catmap.models.Gonderi

data class ProfilePostUiState(
    val posts: List<Gonderi> = emptyList(),
    val postCount: Int = 0,
    val isLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
)