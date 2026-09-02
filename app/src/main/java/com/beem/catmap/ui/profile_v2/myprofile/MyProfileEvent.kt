package com.beem.catmap.ui.profile_v2.myprofile

sealed interface MyProfileEvent {
    data class ShowToast(val message: String) : MyProfileEvent
    data object NavigateToEditProfile : MyProfileEvent
    data object NavigateToAuth : MyProfileEvent
}