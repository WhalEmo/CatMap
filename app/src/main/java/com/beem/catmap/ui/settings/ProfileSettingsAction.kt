package com.beem.catmap.ui.settings

sealed interface ProfileSettingsAction {
    object NavigateBack : ProfileSettingsAction
    object OpenBadges : ProfileSettingsAction
    object OpenBlockedUsers : ProfileSettingsAction
    object RequestSignOut : ProfileSettingsAction
}