package com.beem.catmap.ui.profile_v2.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.ui.theme.PlusJakartaSans
import com.beem.catmap.utils.withPossessiveSuffix

@Composable
fun EquippedBadgeChip(
    badge: EquippedBadgeModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = badge.tier ?: return
    val isTier08 = tier == BadgeTier.TIER_08

    val tierUi = CatMapColors.Badge.getUiColorsForTier(tier.level)

    val strokeColor = if (isTier08) CatMapColors.Badge.Tier08Special.ChipStroke else tierUi.accent
    val bgColor = if (isTier08) CatMapColors.Badge.Tier08Special.ChipBg else tierUi.surface
    val textColor = if (isTier08) CatMapColors.Badge.Tier08Special.ChipText else tierUi.accent

    val title = if (badge.neighborhood.isNotBlank()) {
        "${badge.neighborhood.withPossessiveSuffix()} ${stringResource(tier.titleResId)}"
    } else {
        stringResource(tier.titleResId)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, strokeColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Image(
            painter = painterResource(tier.iconResId),
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontFamily = PlusJakartaSans,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}