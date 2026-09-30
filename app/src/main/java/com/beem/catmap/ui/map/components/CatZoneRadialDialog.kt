package com.beem.catmap.ui.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beem.catmap.R
import com.beem.catmap.ui.map.model.CatClusterItem
import com.beem.catmap.ui.theme.CatMapColors
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PAGE_SIZE = 6

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CatZoneRadialDialog(
    cats: List<CatClusterItem>,
    onDismiss: () -> Unit,
    onCatClick: (CatClusterItem) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = remember(cats.size) {
        if (cats.isEmpty()) 1 else ((cats.size - 1) / PAGE_SIZE) + 1
    }

    val currentChunk = remember(currentPage, cats) {
        cats.chunked(PAGE_SIZE).getOrElse(currentPage) { emptyList() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CatMapColors.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(28.dp),
                color = CatMapColors.SurfaceWhite,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Üst Başlık & Kapatma Butonu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CatMapColors.Badge.SoftCream),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_shield),
                                    contentDescription = null,
                                    tint = CatMapColors.Accent,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bölge Koruma Alanı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CatMapColors.TextPrimary
                                )
                                Text(
                                    text = "${cats.size} Kayıtlı Can Dostumuz",
                                    fontSize = 12.sp,
                                    color = CatMapColors.TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = CatMapColors.Badge.Silver,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Dairesel Spiral Gösterim
                    AnimatedContent(
                        targetState = currentChunk,
                        transitionSpec = { fadeIn(tween(350)) with fadeOut(tween(250)) },
                        label = "RadialPageAnimation"
                    ) { pageCats ->
                        RadialOrbitView(
                            cats = pageCats,
                            onCatClick = onCatClick
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Sayfalama Kontrolleri
                    if (totalPages > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Önceki",
                                    tint = if (currentPage > 0) CatMapColors.Accent else CatMapColors.Divider
                                )
                            }

                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CatMapColors.TextSecondary
                            )

                            IconButton(
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Sonraki",
                                    tint = if (currentPage < totalPages - 1) CatMapColors.Accent else CatMapColors.Divider
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Detaylı profil için fotoğrafa dokunun",
                            fontSize = 11.sp,
                            color = CatMapColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadialOrbitView(
    cats: List<CatClusterItem>,
    onCatClick: (CatClusterItem) -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(cats) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(650, easing = FastOutSlowInEasing)
        )
    }

    // Taşmayı kesin önleyen güvenli sınır (95dp)
    val orbitRadiusDp = 95.dp
    val orbitRadiusPx = with(LocalDensity.current) { orbitRadiusDp.toPx() }
    val itemSizeDp = 56.dp

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // İnce Yörünge Halkası
        Box(
            modifier = Modifier
                .size(orbitRadiusDp * 2)
                .border(
                    width = 1.dp,
                    brush = Brush.radialGradient(
                        listOf(CatMapColors.AccentAlpha20, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        // Merkez Kedi Rozeti
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(CatMapColors.Badge.SoftCream)
                .border(1.5.dp, CatMapColors.AccentAlpha20, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cat_menu),
                contentDescription = null,
                tint = CatMapColors.Accent,
                modifier = Modifier.size(24.dp)
            )
        }

        // Kedilerin Fotoğrafları
        val count = cats.size
        cats.forEachIndexed { index, cat ->
            val angleStep = (2 * Math.PI) / count
            val targetAngle = index * angleStep

            val currentAngle = targetAngle + ((1f - animProgress.value) * Math.PI)
            val currentRadius = orbitRadiusPx * animProgress.value

            val offsetX = (currentRadius * cos(currentAngle)).roundToInt()
            val offsetY = (currentRadius * sin(currentAngle)).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .scale(animProgress.value)
                    .rotate((1f - animProgress.value) * 90f)
                    .size(itemSizeDp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(CatMapColors.SurfaceWhite)
                    .border(2.dp, CatMapColors.Accent, CircleShape)
                    .clickable { onCatClick(cat) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(cat.photoUrl.ifBlank { R.drawable.ic_cat })
                        .crossfade(true)
                        .error(R.drawable.ic_cat)
                        .placeholder(R.drawable.ic_cat)
                        .build(),
                    contentDescription = cat.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}