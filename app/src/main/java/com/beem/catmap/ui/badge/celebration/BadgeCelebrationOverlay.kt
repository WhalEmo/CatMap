package com.beem.catmap.ui.badge.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.ui.manager.BadgeCelebrationPayload
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun BadgeCelebrationOverlay(
    payload: BadgeCelebrationPayload,
    onEquipBadge: (NeighborhoodBadgeModel) -> Unit,
    onDismiss: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.2f) }
    val infiniteTransition = rememberInfiniteTransition(label = "sunburst_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.52f, stiffness = 320f)
        )
    }

    val tierColors = CatMapColors.Badge.getUiColorsForTier(payload.unlockedTier.level)

    // Karartılmış Oyun Arka Planı (%80 Siyah)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Dönen Güneş Işınları Efekti (Sunburst)
        SunburstRays(
            color = tierColors.accent.copy(alpha = 0.28f),
            rotation = rotationAngle,
            modifier = Modifier.size(380.dp)
        )

        // Ana Başarım Kartı
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .scale(scaleAnim.value)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Tıklamayı arka plana kaçırma
                )
        ) {
            // Rozet İkonu ve Işıltı Çerçevesi
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Glow Çemberi
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(tierColors.accent.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )

                Image(
                    painter = painterResource(id = payload.unlockedTier.iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .border(3.5.dp, tierColors.accent, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Üst Başlık Kapsülü
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = tierColors.accent.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, tierColors.accent.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = tierColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (payload.isFirstUnlock) "YENİ BAŞARIM KAZANILDI!" else "SEVİYE YÜKSELTİLDİ!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rozet Başlığı (Örn: Sokak Muhtarı)
            Text(
                text = stringResource(id = payload.unlockedTier.titleResId),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Konum Bilgisi (Örn: İnönü Mahallesi, Soma)
            Text(
                text = payload.badge.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = tierColors.accent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Rozet Kısa Açıklaması
            Text(
                text = stringResource(id = payload.unlockedTier.shortDescriptionResId),
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 🎯 OYUN BUTONU: ROZETİ PROFİLDE KUŞAN
            Button(
                onClick = { onEquipBadge(payload.badge) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tierColors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rozeti Kuşan ve Kullan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kapat / Daha Sonra Butonu
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daha Sonra",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Arkada dönen 12 kanatlı oyun başarım ışınları (Sunburst Rays)
 */
@Composable
private fun SunburstRays(
    color: Color,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.rotate(rotation)
    ) {
        val center = this.center
        val radius = size.minDimension / 2f
        val rayCount = 12
        val sweepAngle = 15f

        for (i in 0 until rayCount) {
            val startAngle = i * (360f / rayCount)
            val path = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius
                    ),
                    startAngleDegrees = startAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = path, color = color)
        }
    }
}