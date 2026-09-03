package com.beem.catmap.ui.profile_v2.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.UserProfileData
import com.beem.catmap.ui.components.CatMapImage
import com.beem.catmap.ui.profile.common.ProfilePreviewHelper
import com.beem.catmap.ui.theme.PlusJakartaSans

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileHeaderSection(
    user: UserProfileData,
    followerCount: Long,
    followingCount: Long,
    onFollowersClick: (() -> Unit)?,
    onFollowingClick: (() -> Unit)?,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    actionButtons: @Composable () -> Unit,
    refreshKey: Any,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Üst Kısım: Avatar ve Sağdaki İsim/İstatistik Bloğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Top // XML'deki topToTopOf hizalaması
        ) {
            CatMapImage(
                data = user.photoUrl,
                contentDescription = "Profil Fotoğrafı",
                refreshKey = refreshKey,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFB0B0B0), CircleShape) // Orijinal XML çerçeve rengi
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            ProfilePreviewHelper.showPreview(context, user.photoUrl)
                        }
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                // Ad Soyad
                Text(
                    text = user.name.ifBlank { "Kullanıcı" },
                    fontFamily = PlusJakartaSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // İstatistikler (Gönderi - Takipçi - Takip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBox(
                        count = user.postCount.toString(),
                        label = "Gönderi",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        count = followerCount.toString(),
                        label = "Takipçi",
                        onClick = onFollowersClick,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        count = followingCount.toString(),
                        label = "Takip",
                        onClick = onFollowingClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Rozet
        user.equippedBadge?.let { badge ->
            if (badge.isValid && badge.tier != null) {
                Spacer(modifier = Modifier.height(8.dp))
                EquippedBadgeChip(badge = badge, onClick = { onBadgeClick(badge) })
            }
        }

        // Biyografi
        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user.bio,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = Color(0xFF262626) // Orijinal XML metin rengi
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aksiyon Butonları
        actionButtons()
    }
}

@Composable
private fun StatBox(
    count: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = count,
            fontFamily = PlusJakartaSans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontFamily = PlusJakartaSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF757575)
        )
    }
}