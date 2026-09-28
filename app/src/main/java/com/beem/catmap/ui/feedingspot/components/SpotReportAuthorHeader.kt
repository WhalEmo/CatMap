package com.beem.catmap.ui.feedingspot.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.ui.components.CatUserAvatar
import com.beem.catmap.ui.feedingspot.FeedingSpotIntent
import com.beem.catmap.ui.feedingspot.model.SpotReportUiModel
import com.beem.catmap.ui.spotoperation.SpotOperationIntent
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.utils.formatTimeAgo
import kotlin.text.ifEmpty

@Composable
fun SpotReportAuthorHeader(
    uiModel: SpotReportUiModel,
    isViewingHistory: Boolean,
    onIntent: (FeedingSpotIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val report = uiModel.report
    val context = LocalContext.current

    val onUserClick = {
        if (report.reporterId.isNotEmpty()) {
            // Intent sınıfınızdaki profil açma event'ini buraya yazın:
            onIntent(FeedingSpotIntent.OpenUserProfile(report.reporterId))
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CatMapColors.SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CatUserAvatar(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onUserClick),
            photoUrl = uiModel.reporterPhotoUrl,
            size = 38.dp,
            isViewingHistory = isViewingHistory,
            contentDescription = "${uiModel.reporterDisplayName} profili"
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@" + uiModel.reporterDisplayName.ifEmpty { "Gönüllü" },
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onUserClick)
            )
            Text(
                text = report.reportedAt.formatTimeAgo(),
                color = CatMapColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        if (isViewingHistory) {
            Surface(
                color = CatMapColors.SurfaceTranslucent,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CatMapColors.Divider)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = CatMapColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Önceki Durum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CatMapColors.TextSecondary
                    )
                }
            }
        } else {
            Surface(
                color = CatMapColors.Accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CatMapColors.Accent.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    // Yeşil/Canlı durum sinyali veren mini nokta
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CatMapColors.Accent)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Son Durum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.Accent
                    )
                }
            }
        }
    }
}