package com.beem.catmap.ui.profile_v2.otherprofile

import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
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
fun OtherProfileScreen(
    uiState: OtherProfileUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: (View) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onChatClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    onPostClick: (Post) -> Unit,
    onLoadMorePosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.user != null

    var refreshTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.username.orEmpty().ifBlank { "Profil" },
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
                    val menuTint = CatMapColors.TextMuted.toArgb()
                    AndroidView(
                        factory = { context ->
                            AppCompatImageButton(
                                context,
                                null,
                                androidx.appcompat.R.attr.actionButtonStyle
                            ).apply {
                                setImageResource(R.drawable.baseline_more_vert_24)
                                setColorFilter(menuTint)
                                setOnClickListener { view -> onMenuClick(view) }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 4.dp)
                    )
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CatMapColors.SurfaceWhite)
            )
        },
        containerColor = CatMapColors.SurfaceWhite,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                refreshTrigger++
                onRefresh()
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
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
                // 1. İLK AÇILIŞ: Shimmer efekti
                uiState.isLoading && uiState.user == null -> {
                    ProfileShimmerLayout()
                }

                // 2. DURUM: Karşı taraf bizi engelledi (Kilitli Ekran)
                uiState.isBlockedByThem -> {
                    BlockedByThemContent(uiState = uiState)
                }

                // 3. DURUM: Sadece ben engellediysem
                uiState.isBlockedByMe && uiState.user != null -> {
                    BlockedByMeContent(
                        uiState = uiState,
                        onUnblockClick = onUnblockClick,
                        onBadgeClick = onBadgeClick
                    )
                }

                // 4. DURUM: Normal Profil Akışı (Takip ediyor veya gizli hesap)
                uiState.user != null -> {
                    val user = uiState.user

                    val canViewFollowLists = !uiState.isAccessDenied && !uiState.isBlockedByMe && !uiState.isBlockedByThem

                    val postUiState = PostUiState(
                        posts = uiState.posts,
                        isLoading = uiState.isPostsLoading,
                        isMoreLoading = uiState.isMoreLoading,
                        isLastPage = uiState.isLastPage,
                        isEmpty = uiState.posts.isEmpty() && !uiState.isPostsLoading && !uiState.isAccessDenied,
                        isAccessDenied = uiState.isAccessDenied
                    )

                    ProfilePostsGrid(
                        headerContent = {
                            ProfileHeaderSection(
                                user = user,
                                followerCount = user.followersCount,
                                followingCount = user.followingCount,
                                onFollowersClick = if (canViewFollowLists) onFollowersClick else null,
                                onFollowingClick = if (canViewFollowLists) onFollowingClick else null,
                                onBadgeClick = onBadgeClick,
                                refreshKey = refreshTrigger,
                                actionButtons = {
                                    OtherProfileActionButtons(
                                        followStatus = uiState.followStatus,
                                        isActionLoading = uiState.isActionLoading,
                                        onFollowClick = onFollowClick,
                                        onUnfollowClick = onUnfollowClick,
                                        onUnblockClick = onUnblockClick,
                                        onChatClick = onChatClick
                                    )
                                }
                            )
                        },
                        postUiState = postUiState,
                        onPostClick = onPostClick,
                        onLoadMore = onLoadMorePosts,
                        refreshKey = refreshTrigger
                    )
                }

                // 5. DURUM: Hata
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
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

/**
 * Kullanıcıyı ben engellediğimde çizilecek temiz ekran.
 */
@Composable
private fun BlockedByMeContent(
    uiState: OtherProfileUiState,
    onUnblockClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit
) {
    var refreshTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val user = uiState.user ?: return

        ProfileHeaderSection(
            user = user,
            followerCount = user.followersCount,
            followingCount = user.followingCount,
            onFollowersClick = null,
            onFollowingClick = null,
            onBadgeClick = onBadgeClick,
            refreshKey = refreshTrigger,
            actionButtons = {
                Button(
                    onClick = onUnblockClick,
                    enabled = !uiState.isActionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CatMapColors.Divider,
                        contentColor = CatMapColors.TextPrimary
                    )
                ) {
                    if (uiState.isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CatMapColors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Engeli Kaldır",
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚫",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Bu kullanıcıyı engellediniz",
                fontFamily = PlusJakartaSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paylaşımlarını, takipçilerini ve hareketlerini görmek için engeli kaldırmalısınız.",
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                color = CatMapColors.TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Takip, Engeli Kaldır, Mesaj butonlarının durum bazlı render edildiği alan
 */
@Composable
private fun OtherProfileActionButtons(
    followStatus: OtherFollowStatus,
    isActionLoading: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onChatClick: () -> Unit,
    height: Dp = 36.dp
) {

    if (isActionLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFFFF9800),
                strokeWidth = 2.dp
            )
        }
        return
    }

    when (followStatus) {
        OtherFollowStatus.LOADING -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFFF9800),
                    strokeWidth = 2.dp
                )
            }
        }

        // Engeli Kaldır Butonu (Turuncu Dolgulu)
        OtherFollowStatus.BLOCKED_BY_ME -> {
            Button(
                onClick = onUnblockClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Engeli kaldır",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Takip Et Butonu (Turuncu Dolgulu)
        OtherFollowStatus.NOT_FOLLOWING -> {
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Takip Et",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Sen de Takip Et Butonu (Turuncu Dolgulu)
        OtherFollowStatus.FOLLOW_BACK -> {
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Sen de Takip Et",
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Takip Ediliyor + Mesaj Yan Yana Çift Buton (Fotoğraftaki Birebir Renkler)
        OtherFollowStatus.FOLLOWING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Takip Butonu (Pastel Yeşil Zemin + Koyu Yeşil Stroke)
                Button(
                    onClick = onUnfollowClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFF556B2F)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC8E6C9),
                        contentColor = Color(0xFF000000)
                    )
                ) {
                    Text(
                        text = "Takip",
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Mesaj Butonu (Pastel Somon Zemin + Koyu Kırmızımsı Stroke)
                Button(
                    onClick = onChatClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, Color(0xFF8F504C)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEB8AE),
                        contentColor = Color(0xFF000000)
                    )
                ) {
                    Text(
                        text = "Mesaj",
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Karşı taraf bizi engellediğinde gösterilecek kilitli profil sayfası.
 */
@Composable
private fun BlockedByThemContent(uiState: OtherProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uiState.user?.photoUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.kullanici),
            error = painterResource(R.drawable.kullanici),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, CatMapColors.Divider, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.user?.username.orEmpty().ifBlank { "Kullanıcı" },
            fontFamily = PlusJakartaSans,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = CatMapColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Kullanıcı Bulunamadı",
            fontFamily = PlusJakartaSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CatMapColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bu hesap kullanılamıyor veya profile erişim izniniz bulunmuyor.",
            fontFamily = PlusJakartaSans,
            fontSize = 14.sp,
            color = CatMapColors.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}