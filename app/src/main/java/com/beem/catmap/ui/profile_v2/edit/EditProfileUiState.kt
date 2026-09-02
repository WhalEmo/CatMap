package com.beem.catmap.ui.profile_v2.edit

import android.net.Uri
import com.beem.catmap.data.model.UserProfileData

data class EditProfileUiState(
    val initialUser: UserProfileData? = null,
    val username: String = "",
    val name: String = "",
    val surname: String = "",
    val bio: String = "",
    val currentPhotoUrl: String = "",
    val selectedImageUri: Uri? = null,
    val usernameError: String? = null,
    val nameError: String? = null,
    val surnameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)