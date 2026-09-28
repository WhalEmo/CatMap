package com.beem.catmap.ui.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.ui.map.model.MapFilterType
import com.beem.catmap.notification.RequestNotificationPermission
import com.beem.catmap.ui.theme.CatMapColors

// Orijinal XML'deki Montserrat-Black fontu
private val MontserratBlack = FontFamily(
    Font(R.font.montserrat_black, FontWeight.Black)
)

@Composable
fun MapTopHeaderBar(
    isScanAreaVisible: Boolean,
    isScanning: Boolean,
    selectedFilter: MapFilterType,
    catCount: Int,
    spotCount: Int,
    onScanAreaClick: () -> Unit,
    onFilterSelected: (MapFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    RequestNotificationPermission()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CatMapTextLogo(
                modifier = Modifier.padding(start = 14.dp, end = 10.dp)
            )
            FilterRowItems(
                selectedFilter,
                catCount,
                spotCount,
                onFilterSelected,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(
            visible = isScanAreaVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !isScanning, onClick = onScanAreaClick),
                color = CatMapColors.SurfaceWhite,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CatMapColors.Divider),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh_minimal),
                        contentDescription = null,
                        tint = CatMapColors.Accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Taranıyor..." else "Bu Alanı Tara",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRowItems(
    selectedFilter: MapFilterType,
    catCount: Int,
    spotCount: Int,
    onFilterSelected: (MapFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(start = 0.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChipItem(
                text = "Tümü",
                badgeCount = catCount + spotCount,
                isSelected = selectedFilter == MapFilterType.ALL,
                iconRes = null,
                onClick = { onFilterSelected(MapFilterType.ALL) }
            )
        }
        item {
            FilterChipItem(
                text = "Kediler",
                badgeCount = catCount,
                isSelected = selectedFilter == MapFilterType.CATS,
                iconRes = R.drawable.ic_cat,
                onClick = { onFilterSelected(MapFilterType.CATS) }
            )
        }

        item {
            FilterChipItem(
                text = "Mama Noktaları",
                badgeCount = spotCount,
                isSelected = selectedFilter == MapFilterType.SPOTS,
                iconRes = R.drawable.ic_cat_food_bowl,
                onClick = { onFilterSelected(MapFilterType.SPOTS) }
            )
        }
    }
}


@Composable
private fun CatMapTextLogo(
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = CatMapColors.Accent,
                    fontFamily = MontserratBlack
                )
            ) {
                append("Cat")
            }
            withStyle(
                style = SpanStyle(
                    color = CatMapColors.TextDark,
                    fontFamily = MontserratBlack
                )
            ) {
                append("Map")
            }
        },
        fontSize = 22.sp,
        letterSpacing = (-0.8).sp,
        style = androidx.compose.ui.text.TextStyle(
            shadow = Shadow(
                color = Color(0x33000000),
                offset = Offset(0f, 3f),
                blurRadius = 6f
            )
        ),
        modifier = modifier
    )
}

@Composable
private fun FilterChipItem(
    text: String,
    badgeCount: Int,
    isSelected: Boolean,
    iconRes: Int?,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) CatMapColors.Accent else CatMapColors.SurfaceWhite
    val contentColor = if (isSelected) Color.White else CatMapColors.TextPrimary
    val borderColor = if (isSelected) CatMapColors.Accent else CatMapColors.Divider

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isSelected) 3.dp else 1.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else CatMapColors.SurfaceTranslucent,
                    shape = CircleShape
                ) {
                    Text(
                        text = "$badgeCount",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}