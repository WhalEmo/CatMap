package com.beem.catmap.ui.navigation

import android.util.Log
import com.beem.catmap.Profil.ProfilFragment
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.ui.message.MessageFragment

object NavigationHelper {
    @JvmStatic
    fun navigateToProfile(targetProfileId: String) {
        Log.d("NAV_BACK_DEDEKTOR", "id: $targetProfileId")
        val screen = if (targetProfileId == UserSession.userId) Screen.PROFILE else Screen.OTHER_PROFILE
        val args = ProfilFragment.newArgs(targetProfileId)
        SmartNavigationEngine.navigateTo(
            targetScreen = screen,
            args = args,
            key = targetProfileId
        )
    }

    @JvmStatic
    fun navigateToChat(receiverId: String) {
        val args = MessageFragment.newArgs(receiverId)

        SmartNavigationEngine.navigateTo(
            targetScreen = Screen.MESSAGE,
            args,
            key = receiverId
        )
    }
}