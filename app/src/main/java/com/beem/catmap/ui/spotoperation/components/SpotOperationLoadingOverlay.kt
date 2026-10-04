package com.beem.catmap.ui.spotoperation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.ui.theme.CatMapColors
import kotlinx.coroutines.delay
import com.beem.catmap.R
import com.beem.catmap.ui.spotoperation.SpotOperationIntent
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SpotOperationLoadingOverlay(
    modifier: Modifier = Modifier,
    onIntent: (SpotOperationIntent) -> Unit
) {

    val infiniteTransition = rememberInfiniteTransition(label = "CatPulseTransition")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CatPulseScale"
    )
    // 🐾 Kullanıcıyı noktada tutmaya ve bilgilendirmeye yönelik mesaj listesi
    val loadingMessages = remember {
        listOf(
            "Fotoğrafınız ve bildiriminiz şifrelenerek hazırlanıyor...",
            "Lütfen işlem bitene kadar noktadan ayrılmayın (50m Kalkanı Aktif) 🛡️",
            "Bu kare sokaktaki dostlarımızın beslenme haritasını güncelliyor...",
            "Topluluğumuz için çok değerlisiniz, az kaldı...",
            "Besleme noktası veritabanına mühürleniyor... 🐾"
        )
    }

    var messageIndex by remember { mutableIntStateOf(0) }

    // Her 2.5 saniyede bir yazıyı yumuşakça değiştir
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500.milliseconds)
            messageIndex = (messageIndex + 1) % loadingMessages.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CatMapColors.SurfaceWhite)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animasyonlu Kedi/İkon Alanı
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(CatMapColors.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cat_menu),
                    contentDescription = null,
                    tint = CatMapColors.Accent,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Operasyon Kaydediliyor",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🚀 Yumuşak Geçişli Metin Bloğu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = loadingMessages[messageIndex],
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 2 })
                            .togetherWith(fadeOut() + slideOutVertically { -it / 2 })
                    },
                    label = "LoadingMessageAnim"
                ) { targetMessage ->
                    Text(
                        text = targetMessage,
                        fontSize = 14.sp,
                        color = CatMapColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🚀 Modern ve Yuvarlatılmış Linear Progress Bar
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = CatMapColors.Accent,
                trackColor = CatMapColors.Accent.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )
        }
        TextButton(
            onClick = { onIntent(SpotOperationIntent.UploadReportCancel) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = null,
                    tint = CatMapColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "İşlemi İptal Et",
                    color = CatMapColors.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}