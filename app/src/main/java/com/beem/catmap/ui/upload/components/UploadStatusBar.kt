package com.beem.catmap.ui.upload.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.ui.upload.UploadStage

@Composable
fun UploadStatusBar(
    stage: UploadStage,
    progress: Int,
    errorMessage: String?,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = stage != UploadStage.IDLE,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = progress / 100f,
            label = "UploadProgressBar"
        )

        val shape = RoundedCornerShape(16.dp)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            shape = shape,
            color = CatMapColors.SurfaceWhite,
            border = BorderStroke(1.dp, CatMapColors.Divider),
            shadowElevation = 6.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sol Taraf: İkon + Durum Metni
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusIconContainer(stage = stage)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            val title = when (stage) {
                                UploadStage.FETCHING_LOCATION -> "Konum Senkronizasyonu"
                                UploadStage.UPLOADING_ASSETS -> "Buluta Yükleniyor"
                                UploadStage.SUCCESS -> "Haritaya Eklendi"
                                UploadStage.ERROR -> "Yükleme Başarısız"
                                UploadStage.IDLE -> ""
                            }

                            val description = when (stage) {
                                UploadStage.FETCHING_LOCATION -> "GPS uyduları ile eşleşiyor..."
                                UploadStage.UPLOADING_ASSETS -> "Fotoğraflar ve bilgiler aktarılıyor..."
                                UploadStage.SUCCESS -> "Kedi harita üzerinde yayında."
                                UploadStage.ERROR -> errorMessage ?: "Bilinmeyen bir hata oluştu."
                                UploadStage.IDLE -> ""
                            }

                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stage == UploadStage.ERROR) CatMapColors.Error else CatMapColors.TextPrimary
                            )

                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = if (stage == UploadStage.ERROR) CatMapColors.Error.copy(alpha = 0.85f) else CatMapColors.TextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sağ Taraf: Progress Badge veya İptal/Kapat Aksiyonu
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stage == UploadStage.UPLOADING_ASSETS) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CatMapColors.AccentAlpha15,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "%$progress",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CatMapColors.Accent,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (stage == UploadStage.ERROR) onDismissError() else onCancel()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Kapat",
                                tint = CatMapColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Yükleme Aşaması Alt Progress Barı
                if (stage == UploadStage.UPLOADING_ASSETS) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CatMapColors.Accent,
                        trackColor = CatMapColors.AccentAlpha15
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIconContainer(stage: UploadStage) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                when (stage) {
                    UploadStage.FETCHING_LOCATION -> CatMapColors.AccentAlpha15
                    UploadStage.UPLOADING_ASSETS -> CatMapColors.AccentAlpha15
                    UploadStage.SUCCESS -> CatMapColors.Success.copy(alpha = 0.15f)
                    UploadStage.ERROR -> CatMapColors.Error.copy(alpha = 0.15f)
                    UploadStage.IDLE -> CatMapColors.Transparent
                }
            )
    ) {
        when (stage) {
            UploadStage.FETCHING_LOCATION -> {
                Icon(
                    painter = painterResource(R.drawable.ic_location_off),
                    contentDescription = null,
                    tint = CatMapColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            UploadStage.UPLOADING_ASSETS -> {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    tint = CatMapColors.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            UploadStage.SUCCESS -> {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = CatMapColors.Success,
                    modifier = Modifier.size(22.dp)
                )
            }
            UploadStage.ERROR -> {
                Icon(
                    painter = painterResource(R.drawable.ic_error_outline),
                    contentDescription = null,
                    tint = CatMapColors.Error,
                    modifier = Modifier.size(22.dp)
                )
            }
            UploadStage.IDLE -> Unit
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun UploadStatusBarLoadingPreview() {
    UploadStatusBar(
        stage = UploadStage.UPLOADING_ASSETS,
        progress = 72,
        errorMessage = null,
        onCancel = {},
        onDismissError = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun UploadStatusBarSuccessPreview() {
    UploadStatusBar(
        stage = UploadStage.SUCCESS,
        progress = 100,
        errorMessage = null,
        onCancel = {},
        onDismissError = {},
        modifier = Modifier.padding(16.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun UploadStatusBarErrorPreview() {
    UploadStatusBar(
        stage = UploadStage.ERROR,
        progress = 0,
        errorMessage = "GPS uydusuna bağlanılamadı, konum açık değil.",
        onCancel = {},
        onDismissError = {},
        modifier = Modifier.padding(16.dp)
    )
}