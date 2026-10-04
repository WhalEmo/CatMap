package com.beem.catmap.ui.feedingspot.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.data.model.SpotState
import com.beem.catmap.ui.feedingspot.model.SpotReportUiModel
import com.beem.catmap.ui.feedingspot.toUiBadge
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.utils.formatExactTime
import kotlin.text.ifEmpty

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpotReportStatusCard(
    spot: FeedingSpot,
    uiModel: SpotReportUiModel,
    isViewingHistory: Boolean,
    decayBadgeText: String,
    decayBadgeContentColor: androidx.compose.ui.graphics.Color,
    decayBadgeContainerColor: androidx.compose.ui.graphics.Color,
    decayBadgeBorderColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val report = uiModel.report
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CatMapColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Ana Durum Rozeti
            val statusBadge = SpotState.safeValueOf(spot.currentStatus).toUiBadge()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isViewingHistory) CatMapColors.SurfaceTranslucent else statusBadge.containerColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (isViewingHistory) CatMapColors.Divider else statusBadge.borderColor)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isViewingHistory) R.drawable.ic_clock else statusBadge.iconResId),
                        contentDescription = null,
                        tint = if (isViewingHistory) CatMapColors.TextMuted else statusBadge.contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isViewingHistory) "Önceki Durum" else statusBadge.text,
                        color = if (isViewingHistory) CatMapColors.TextMuted else statusBadge.contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zaman Şeridi
            val stripContainer = if (isViewingHistory) CatMapColors.SurfaceTranslucent else decayBadgeContainerColor
            val stripBorder = if (isViewingHistory) CatMapColors.Divider else decayBadgeBorderColor
            val stripTextTint = if (isViewingHistory) CatMapColors.TextSecondary else decayBadgeContentColor

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, stripBorder, RoundedCornerShape(8.dp)),
                color = stripContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isViewingHistory) Arrangement.Center else Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            tint = stripTextTint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isViewingHistory) "İşlem Saati: ${report.reportedAt.formatExactTime()}"
                            else "Son İşlem: ${report.reportedAt.formatExactTime()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = stripTextTint
                        )
                    }
                    if (!isViewingHistory) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(decayBadgeContentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = decayBadgeText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = decayBadgeContentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CatMapColors.Divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Müdahaleler & Gözlemler",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                report.reportTags.forEach { actionCode ->
                    val actionBadge = ReportAction.safeValueOf(actionCode).toUiBadge()
                    Surface(
                        color = actionBadge.containerColor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, actionBadge.borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = actionBadge.iconResId),
                                contentDescription = null,
                                tint = actionBadge.contentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = actionBadge.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CatMapColors.TextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rapor Notu",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.note.ifEmpty { "Not eklenmemiş." },
                fontSize = 14.sp,
                color = CatMapColors.TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}