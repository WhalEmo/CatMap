package com.beem.catmap.ui.profile_v2.myprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.badge.BadgeStoryBottomSheet
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine

class MyProfileFragment : Fragment() {

    private val viewModel: MyProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        viewModel.event.collect { event ->
                            when (event) {
                                is MyProfileEvent.ShowToast -> {
                                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                                }
                                is MyProfileEvent.NavigateToEditProfile -> {
                                    SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE)
                                }
                                is MyProfileEvent.NavigateToAuth -> {
                                    SmartNavigationEngine.resetEngineForLogout(Screen.AUTH)
                                }
                            }
                        }
                    }

                    MyProfileScreen(
                        uiState = state,
                        onRefresh = {
                            viewModel.loadMyProfile(isRefresh = true)
                            viewModel.loadPosts(isRefresh = true)
                        },
                        onBackClick = { SmartNavigationEngine.navigateBack() },
                        onMenuClick = { showMyProfileOptionMenu(it) },
                        onEditProfileClick = { SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE) },
                        onFollowersClick = { navigateToFollowList(0) },
                        onFollowingClick = { navigateToFollowList(1) },
                        onBadgeClick = { badge ->
                            BadgeStoryBottomSheet.show(childFragmentManager, badge)
                        },
                        onPostClick = { post -> onPostSelected(post) },
                        onLoadMorePosts = { viewModel.loadMorePosts() }
                    )
                }
            }
        }
    }

    private fun navigateToFollowList(startPage: Int) {
        val myId = UserSession.userId.orEmpty()
        val args = bundleOf(
            "yukleyenID" to myId,
            "startPage" to startPage
        )
        SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, args, "${myId}_$startPage")
    }

    private fun onPostSelected(post: Post) {
        val args = bundleOf(
            "kediid" to post.catId,
            "yukleyenId" to UserSession.userId.orEmpty()
        )
        SmartNavigationEngine.navigateTo(Screen.POST, args, post.catId)
    }

    private fun showMyProfileOptionMenu(anchorView: View) {
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.catmap_text_primary)

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = 4,
                title = "Rozetler",
                iconRes = R.drawable.catmap_badge_tier_05,
                isVisible = true
            ) {
                SmartNavigationEngine.navigateTo(Screen.BADGE)
            }
            .addItem(
                id = R.id.engellenenlerGetir,
                title = "Engellenenler",
                iconRes = R.drawable.exit,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = true
            ) {
                SmartNavigationEngine.navigateTo(Screen.BLOCKED_USERS)
            }
            .addItem(
                id = R.id.signOut,
                title = "Çıkış Yap",
                iconRes = R.drawable.logout,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = true
            ) {
                if (childFragmentManager.findFragmentByTag("SignOutDialog") == null) {
                    CatMapDialog.build()
                        .setTitle("Maceraya Mola mı?")
                        .setMessage("Dostlarımız haritada seni bekliyor olacak! Yine bekleriz, çıkış yapmak istediğine emin misin?")
                        .setPositiveButton("Evet, Çıkış Yap") {
                            viewModel.logout()
                        }
                        .setNegativeButton("Kalıyorum")
                        .show(childFragmentManager, "SignOutDialog")
                }
            }
            .build()
            .show(anchorView = anchorView)
    }
}