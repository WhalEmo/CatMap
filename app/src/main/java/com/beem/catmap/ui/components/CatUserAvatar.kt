package com.beem.catmap.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun CatUserAvatar(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    isViewingHistory: Boolean = false,
    contentDescription: String? = "Kullanıcı Profili",
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val backgroundColor = if (isViewingHistory) {
        CatMapColors.SurfaceTranslucent
    } else {
        CatMapColors.Accent.copy(alpha = 0.12f)
    }

    val iconTint = if (isViewingHistory) {
        CatMapColors.TextMuted
    } else {
        CatMapColors.Accent
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = !photoUrl.isNullOrBlank(),
            animationSpec = tween(300),
            label = "AvatarCrossfade"
        ) { hasPhoto ->
            if (hasPhoto) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    loading = {
                        AvatarPlaceholder(iconTint = iconTint, iconSize = size * 0.5f)
                    },
                    error = {
                        AvatarPlaceholder(iconTint = iconTint, iconSize = size * 0.5f)
                    }
                )
            } else {
                AvatarPlaceholder(iconTint = iconTint, iconSize = size * 0.5f)
            }
        }
    }
}

@Composable
private fun AvatarPlaceholder(
    iconTint: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_person),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = iconTint
        )
    }
}