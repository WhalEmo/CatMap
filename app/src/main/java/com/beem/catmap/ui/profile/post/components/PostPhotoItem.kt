package com.beem.catmap.ui.profile.post.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun PostPhotoItem(
    url: String,
    onPhotoClick: () -> Unit
) {
    val context = LocalContext.current
    var retryTrigger by remember { mutableIntStateOf(0) }

    val imageRequest = remember(url, retryTrigger) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .crossfade(450)
            .memoryCachePolicy(if (retryTrigger > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // Eski koddaki 800ms Decelerate Reverse Pulse Animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CatMapColors.SurfaceTranslucent),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Gönderi Fotoğrafı",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        ) {
            when (val state = painter.state) {

                // 1. YÜKLENİYOR DURUMU: Nefes alan pulse görseli
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CatMapColors.Background)
                            .alpha(pulseAlpha),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_cat_menu),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp), // Boyutu buradan istediğin gibi ayarla (örn: 56.dp, 64.dp, 72.dp)
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // 2. BAŞARILI DURUM: Tıklanınca tam ekran açılır
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onPhotoClick
                            )
                    )
                }

                // 3. HATA DURUMU: Profesyonel CatMap hata kalkanı + Retry
                is AsyncImagePainter.State.Error -> {
                    val errorMsg = state.result.throwable.message ?: ""
                    val isAuthError = errorMsg.contains("401") || errorMsg.contains("403") ||
                            errorMsg.contains("Unauthorized") || errorMsg.contains("Forbidden")

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CatMapColors.SurfaceTranslucent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { if (!isAuthError) retryTrigger++ }
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cat),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = CatMapColors.Error.copy(alpha = 0.55f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isAuthError) {
                            Text(
                                text = "Görsele erişim izni bulunamadı.",
                                color = CatMapColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Fotoğraf yüklenemedi",
                                color = CatMapColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tekrar denemek için dokunun",
                                color = CatMapColors.TextMuted,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { retryTrigger++ },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CatMapColors.Accent
                                ),
                                border = BorderStroke(1.dp, CatMapColors.Accent),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tekrar Dene",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. BOŞ DURUM (URL null/boş ise)
                is AsyncImagePainter.State.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cat),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CatMapColors.TextMuted.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}