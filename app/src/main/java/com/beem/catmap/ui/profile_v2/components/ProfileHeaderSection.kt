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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beem.catmap.R
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.UserProfileData
import com.beem.catmap.ui.profile.common.ProfilePreviewHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileHeaderSection(
    user: UserProfileData,
    followerCount: Long,
    followingCount: Long,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    actionButtons: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Profil Fotoğrafı",
                placeholder = painterResource(R.drawable.kullanici),
                error = painterResource(R.drawable.kullanici),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFB0B0B0), CircleShape)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            ProfilePreviewHelper.showPreview(context, user.photoUrl)
                        }
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "Kullanıcı" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox(count = user.postCount.toString(), label = "Gönderi")
                    StatBox(count = followerCount.toString(), label = "Takipçi", onClick = onFollowersClick)
                    StatBox(count = followingCount.toString(), label = "Takip", onClick = onFollowingClick)
                }
            }
        }

        user.equippedBadge?.let { badge ->
            if (badge.isValid && badge.tier != null) {
                Spacer(modifier = Modifier.height(8.dp))
                EquippedBadgeChip(badge = badge, onClick = { onBadgeClick(badge) })
            }
        }

        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user.bio,
                fontSize = 14.sp,
                color = Color(0xFF262626)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        actionButtons()
    }
}

@Composable
private fun StatBox(
    count: String,
    label: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp)
    ) {
        Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}