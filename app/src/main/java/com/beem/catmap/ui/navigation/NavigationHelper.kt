package com.beem.catmap.ui.navigation

import android.util.Log
import com.beem.catmap.MainActivity
import com.beem.catmap.Profil.ProfilSayfasiFragment

object NavigationHelper {
    @JvmStatic
    fun navigateToProfile(targetProfileId: String) {
        Log.d("NAV_BACK_DEDEKTOR", "id: $targetProfileId")
        val screen = if (targetProfileId == MainActivity.kullanici.id) Screen.PROFILE else Screen.OTHER_PROFILE
        val args = ProfilSayfasiFragment.newArgs(targetProfileId)
        SmartNavigationEngine.navigateTo(
            targetScreen = screen,
            args = args,
            key = targetProfileId
        )
    }
}