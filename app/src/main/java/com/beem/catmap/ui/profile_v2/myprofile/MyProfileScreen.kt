package com.beem.catmap.ui.profile_v2.myprofile

import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.beem.catmap.R
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.profile.post.PostUiState
import com.beem.catmap.ui.profile_v2.components.ProfileHeaderSection
import com.beem.catmap.ui.profile_v2.components.ProfilePostsGrid
import com.beem.catmap.ui.profile_v2.components.ProfileShimmerLayout
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.ui.theme.PlusJakartaSans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    uiState: MyProfileUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: (View) -> Unit,
    onEditProfileClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    onPostClick: (Post) -> Unit,
    onLoadMorePosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    var refreshTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.username.orEmpty().ifBlank { "Profilim" },
                        fontFamily = PlusJakartaSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = CatMapColors.TextPrimary
                        )
                    }
                },
                actions = {
                    val menuIconColor = CatMapColors.TextMuted.toArgb()
                    AndroidView(
                        factory = { context ->
                            AppCompatImageButton(
                                context,
                                null,
                                androidx.appcompat.R.attr.actionButtonStyle
                            ).apply {
                                setImageResource(R.drawable.baseline_more_vert_24)
                                setColorFilter(menuIconColor)

                                setOnClickListener { view ->
                                    onMenuClick(view)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 4.dp)
                    )
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CatMapColors.SurfaceWhite
                )
            )
        },
        containerColor = CatMapColors.SurfaceWhite,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                refreshTrigger++
                onRefresh()
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    containerColor = CatMapColors.SurfaceWhite,
                    color = CatMapColors.Accent,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 1. İLK YÜKLEME: Dönen halka yerine detaylı Shimmer İskeleti
                uiState.isLoading && uiState.user == null -> {
                    ProfileShimmerLayout()
                }

                // 2. PROFİL YÜKLENDİ: Gerçek içerik
                uiState.user != null -> {
                    val user = uiState.user

                    val postUiState = PostUiState(
                        posts = uiState.posts,
                        isLoading = uiState.isPostsLoading,
                        isMoreLoading = uiState.isMoreLoading,
                        isLastPage = uiState.isLastPage,
                        isEmpty = uiState.posts.isEmpty() && !uiState.isPostsLoading,
                        isAccessDenied = false
                    )

                    ProfilePostsGrid(
                        headerContent = {
                            ProfileHeaderSection(
                                user = user,
                                followerCount = user.followersCount,
                                followingCount = user.followingCount,
                                onFollowersClick = onFollowersClick,
                                onFollowingClick = onFollowingClick,
                                refreshKey = refreshTrigger,
                                onBadgeClick = onBadgeClick,
                                actionButtons = {
                                    Button(
                                        onClick = onEditProfileClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFE0B2),
                                            contentColor = Color(0xFF000000)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                                    ) {
                                        Text(
                                            text = "Profili düzenle",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            )
                        },
                        postUiState = postUiState,
                        onPostClick = onPostClick,
                        onLoadMore = onLoadMorePosts,
                        refreshKey = refreshTrigger
                    )
                }

                // 3. HATA DURUMU
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium,
                            color = CatMapColors.Error,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}