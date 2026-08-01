package com.beem.catmap.ui.navigation

import android.util.Log
import com.beem.catmap.MainActivity
import com.beem.catmap.Profil.ProfilFragment
import com.beem.catmap.data.local.UserSession

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
}