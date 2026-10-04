package com.beem.catmap.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.beem.catmap.main.AppShellIntent
import com.beem.catmap.main.AppShellViewModel
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine

class ProfileSettingsFragment : Fragment() {
    private val appShellViewModel: AppShellViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ProfileSettingsScreen(
                    appVersion = getAppVersionName(),
                    onAction = { action ->
                        handleAction(action)
                    }
                )
            }
        }
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

    private fun handleAction(action: ProfileSettingsAction) {
        when (action) {
            ProfileSettingsAction.NavigateBack -> {
                SmartNavigationEngine.navigateBack()
            }
            ProfileSettingsAction.OpenBadges -> {
                SmartNavigationEngine.navigateTo(Screen.BADGE)
            }
            ProfileSettingsAction.OpenBlockedUsers -> {
                SmartNavigationEngine.navigateTo(Screen.BLOCKED_USERS)
            }
            ProfileSettingsAction.RequestSignOut -> {
                showSignOutDialog()
            }
        }
    }

    private fun showSignOutDialog() {
        if (childFragmentManager.findFragmentByTag("SignOutDialog") == null) {
            CatMapDialog.build()
                .setTitle("Maceraya Mola mı?")
                .setMessage("Dostlarımız haritada seni bekliyor olacak! Yine bekleriz, çıkış yapmak istediğine emin misin?")
                .setPositiveButton("Evet, Çıkış Yap") {
                    appShellViewModel.onIntent(AppShellIntent.SignOut)
                }
                .setNegativeButton("Kalıyorum")
                .show(childFragmentManager, "SignOutDialog")
        }
    }

    companion object {
        fun newInstance() = ProfileSettingsFragment()
    }
}