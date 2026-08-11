package com.beem.catmap.ui.navigation

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.beem.catmap.Profil.ProfilFragment
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.ui.message.MessageFragment
import com.beem.catmap.ui.report.ReportBottomSheetFragment
import com.beem.catmap.ui.report.ReportType

object NavigationHelper {
    @JvmStatic
    fun navigateToProfile(targetProfileId: String, isFollowed: Boolean = false) {
        Log.d("NAV_BACK_DEDEKTOR", "id: $targetProfileId, isFollowed: $isFollowed")
        val screen = if (targetProfileId == UserSession.userId) Screen.PROFILE else Screen.OTHER_PROFILE

        val args = ProfilFragment.newArgs(targetProfileId).apply {
            putBoolean("IS_FOLLOWED", isFollowed)
        }

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


    @JvmStatic
    fun showReportBottomSheet(
        fragmentManager: FragmentManager,
        targetId: String,
        reportType: ReportType
    ) {
        val bottomSheet = ReportBottomSheetFragment.newInstance(targetId, reportType)
        bottomSheet.show(fragmentManager, "ReportBottomSheetFragment")
    }
}