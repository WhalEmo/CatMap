package com.beem.catmap.ui.profile_v2.edit

sealed interface EditProfileEvent {
    data object SaveSuccess : EditProfileEvent
}