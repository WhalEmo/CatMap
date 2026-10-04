package com.beem.catmap.ui.spotoperation

import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beem.catmap.R
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.engine.location.LocationAccessState
import com.beem.catmap.ui.feedingspot.toUiBadge
import com.beem.catmap.ui.spotoperation.components.LocationPermissionRequiredOverlay
import com.beem.catmap.ui.spotoperation.components.SpotOperationLoadingOverlay
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.utils.DistanceFormat
import com.beem.catmap.utils.toSmartDistanceString

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SpotOperationScreen(
    state: SpotOperationUiState,
    snackBarHostState: SnackbarHostState,
    onIntent: (SpotOperationIntent) -> Unit
) {
    val scrollState = rememberScrollState()

    // Akıllı Buton Metni Hesaplamaları
    val addedFood = state.selectedActions.contains(ReportAction.ACTION_ADDED_FOOD) || state.selectedActions.contains(ReportAction.ACTION_ADDED_BOTH)
    val addedWater = state.selectedActions.contains(ReportAction.ACTION_ADDED_WATER) || state.selectedActions.contains(ReportAction.ACTION_ADDED_BOTH)
    val hasObservation = state.selectedActions.any { it.name.startsWith("OBSERVED_FOOD") || it.name.startsWith("OBSERVED_WATER") }

    val bottomButtonText = when (state.currentStep) {
        1 -> if (addedFood || addedWater) "İlerle" else "Sadece Gözlem Yaptım"
        2 -> if (hasObservation) "İlerle" else "Durumu Bilmiyorum (Geç)"
        else -> if (state.isCreateMode) "Noktayı Oluştur ve Paylaş" else "Güncellemeyi Paylaş"
    }

    val isNextEnabled = !state.isUploading

    Scaffold(
        topBar = {
            OperationTopBar(onBackClick = { onIntent(SpotOperationIntent.NavigateBack) })
        },
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                val isSuccess = data.visuals.actionLabel == "true"
                val bgColor = if (isSuccess) CatMapColors.Success else CatMapColors.Error
                val iconRes = if (isSuccess) R.drawable.ic_check else R.drawable.ic_error_outline

                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    color = bgColor,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = data.visuals.message,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        bottomBar = {
            val isFooterVisible = !state.isLoadingRadar &&
                    state.shieldState == ShieldState.VERIFIED &&
                    !state.isUploading

            AnimatedVisibility(
                visible = isFooterVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut()
            ) {
                WizardFooter(
                    currentStep = state.currentStep,
                    text = bottomButtonText,
                    isEnabled = isNextEnabled,
                    isUploading = state.isUploading,
                    onBackClick = { onIntent(SpotOperationIntent.PreviousStep) },
                    onNextClick = {
                        if (state.currentStep < 3) onIntent(SpotOperationIntent.NextStep)
                        else onIntent(SpotOperationIntent.SubmitReport)
                    }
                )
            }
        },
        containerColor = CatMapColors.SurfaceWhite
    ) { paddingValues ->
        AnimatedContent(
            targetState = when {
                state.locationAccessState != LocationAccessState.READY -> "PERMISSION"
                state.isUploading -> "UPLOADING"
                else -> "CONTENT"
            },
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "UploadTransition"
        ) { screenMode ->

            when (screenMode) {
                "PERMISSION" -> {
                    LocationPermissionRequiredOverlay(
                        accessState = state.locationAccessState,
                        onIntent = onIntent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                "UPLOADING" -> {
                    SpotOperationLoadingOverlay(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        onIntent = onIntent
                    )
                }
                "CONTENT" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (state.isLoadingRadar) {
                            // ⏳ YÜKLENİYOR DURUMU
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CatMapColors.Accent)
                            }
                        } else {

                            if (state.isLockedMode) {
                                LockedTargetCard(spotName = state.spotName, distance = state.distanceInMeters)
                            } else {
                                RadarCarousel(
                                    nearbySpots = state.nearbySpots,
                                    isCreateAllowed = state.isCreateAllowed,
                                    selectedSpotId = state.selectedSpotId,
                                    onSelect = { onIntent(SpotOperationIntent.SelectSpot(it)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. FAZ: DURUM KONTROLÜ (Form mu açılacak, Hata mı verilecek?)
                            if (state.shieldState == ShieldState.TOO_FAR || state.shieldState == ShieldState.ERROR) {
                                // ❌ UZAKLIK VEYA GPS HATASI UYARISI
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    DistanceWarningCard(state.shieldState, state.distanceInMeters, onRetry = { onIntent(SpotOperationIntent.RetryShield) })
                                }
                            } else if (state.shieldState == ShieldState.VERIFIED) {
                                // ✅ ONAYLI ALAN - SİHİRBAZ BAŞLIYOR
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                                    // YENİ NOKTAYSA İSİM ZORUNLU
                                    if (state.isCreateMode) {
                                        SectionTitle("Noktaya Bir İsim Ver (Zorunlu)", "Diğer gönüllülerin burayı kolayca bulabilmesi için akılda kalıcı bir isim belirle.")
                                        OutlinedTextField(
                                            value = state.newSpotName,
                                            onValueChange = { onIntent(SpotOperationIntent.UpdateNewSpotName(it)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Örn: Mavi Evin Önü, Çınar Ağacı Altı...", color = CatMapColors.TextMuted, fontSize = 13.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = CatMapColors.Accent,
                                                unfocusedBorderColor = CatMapColors.Divider,
                                                focusedContainerColor = CatMapColors.SurfaceWhite,
                                                unfocusedContainerColor = CatMapColors.SurfaceWhite
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    // FOTOĞRAF HER ZAMAN EN ÜSTTE ZORUNLU
                                    SectionTitle("Güncel Fotoğraf (Zorunlu)", "Noktanın durumunu diğer hayvanseverlerle paylaşmak için anlık fotoğraf eklemelisin.")
                                    PhotoRequirementCard(
                                        photoUri = state.photoUri,
                                        onCameraClick = { onIntent(SpotOperationIntent.OpenCamera) },
                                        onRemovePhoto = { onIntent(SpotOperationIntent.RemovePhoto) }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    StepProgressIndicator(currentStep = state.currentStep, totalSteps = 3)

                                    // SİHİRBAZ ADIMLARI
                                    AnimatedContent(
                                        targetState = state.currentStep,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn() togetherWith
                                                        slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()
                                            } else {
                                                slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn() togetherWith
                                                        slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()
                                            }
                                        },
                                        label = "wizard_steps"
                                    ) { step ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            when (step) {
                                                1 -> Step1Feeding(state.selectedActions, onIntent)
                                                2 -> Step2Observation(state.selectedActions, addedFood, addedWater, onIntent)
                                                3 -> Step3EnvironmentAndNotes(state, onIntent)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================================
// 🚀 YENİLİK: AKILLI RADAR (CAROUSEL) TASARIMI
// ====================================================================================

@Composable
fun RadarCarousel(
    nearbySpots: List<SpotDistanceItem>,
    isCreateAllowed: Boolean,
    selectedSpotId: String?,
    onSelect: (String?) -> Unit
) {
    Column {
        Text(
            text = "Yakındaki Hedefler",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = CatMapColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KURAL 1: Yeni Ekleme Kartı (Eğer Anti-Spam izin veriyorsa)
            if (isCreateAllowed) {
                item {
                    SelectionCard(
                        title = "Yeni Nokta Ekle",
                        subtitle = "Bulunduğun konum",
                        iconRes = R.drawable.ic_add, // Varsa artı ikonu koy, yoksa ic_map_pin kullan
                        isSelected = selectedSpotId == null,
                        onClick = { onSelect(null) }
                    )
                }
            }

            // KURAL 2: Var olan yakındaki noktalar
            items(nearbySpots) { item ->
                SelectionCard(
                    title = item.spot.spotName,
                    subtitle = item.distance.toSmartDistanceString("?"),
                    iconRes = R.drawable.ic_location_puck,
                    isSelected = selectedSpotId == item.spot.id,
                    onClick = { onSelect(item.spot.id) }
                )
            }
        }
    }
}

@Composable
fun SelectionCard(title: String, subtitle: String, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) CatMapColors.Accent.copy(alpha = 0.1f) else CatMapColors.SurfaceWhite
    val borderColor = if (isSelected) CatMapColors.Accent else CatMapColors.Divider
    val iconColor = if (isSelected) CatMapColors.Accent else CatMapColors.TextSecondary

    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, fontSize = 11.sp, color = CatMapColors.TextSecondary)
        }
    }
}

// 🚀 YENİLİK: UZAKLIK / HATA UYARISI
@Composable
fun DistanceWarningCard(shieldState: ShieldState, distance: Int?, onRetry: () -> Unit) {
    val (bgColor, contentColor, iconRes, title, desc) = if (shieldState == ShieldState.TOO_FAR) {
        val smartDistance = distance.toSmartDistanceString(
            fallback = "?",
            format = DistanceFormat.LONG,
            suffix = ""
        )
        listOf(CatMapColors.Error.copy(alpha = 0.1f), CatMapColors.Error, R.drawable.ic_error_outline,
            "Biraz Daha Yaklaşmalısın", "Bu noktaya $smartDistance uzaksın. Güncelleme yapabilmek için 50 metre yakınına gitmelisin.")
    } else {
        listOf(CatMapColors.Badge.Gold.copy(alpha = 0.15f), CatMapColors.Badge.Gold, R.drawable.ic_warning,
            "GPS Sinyali Yok", "Konum servislerini açıp tekrar deneyebilirsin.")
    }

    Surface(modifier = Modifier.fillMaxWidth(), color = bgColor as Color, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, (contentColor as Color).copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = iconRes as Int), contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title as String, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 15.sp)
                Text(text = desc as String, color = CatMapColors.TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            IconButton(onClick = onRetry) { Icon(painterResource(id = R.drawable.ic_refresh_minimal), contentDescription = "Tekrar Dene", tint = contentColor) }
        }
    }
}

// ====================================================================================
// SİHİRBAZ ADIMLARI (STEPS) & YARDIMCILAR (Eskisiyle tamamen aynı)
// ====================================================================================

@Composable
fun Step1Feeding(selectedActions: List<ReportAction>, onIntent: (SpotOperationIntent) -> Unit) {
    Column {
        ChipSection(
            title = "Patili Dostlarımızı Besledin mi?",
            subtitle = "Bir kap mama veya bir yudum su onların gününü güzelleştirir. Neler eklediğini işaretle.",
            actions = listOf(ReportAction.ACTION_ADDED_BOTH, ReportAction.ACTION_ADDED_FOOD, ReportAction.ACTION_ADDED_WATER),
            selectedActions = selectedActions,
            onToggle = { onIntent(SpotOperationIntent.ToggleAction(it)) }
        )
    }
}

@Composable
fun Step2Observation(selectedActions: List<ReportAction>, addedFood: Boolean, addedWater: Boolean, onIntent: (SpotOperationIntent) -> Unit) {
    Column {
        val title = if (addedFood || addedWater) "Peki Diğer Kap Nasıldı?" else "Noktanın Mevcut Durumu"
        val subtitle = if (addedFood || addedWater) "Harika bir iş çıkardın! Eksik olan kabın durumunu da topluluğa bildirir misin?" else "Gözlemlerin hayat kurtarır! Mevcut durumu bildirmen diğer gönüllüleri hemen harekete geçirecektir."
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary)
        Text(text = subtitle, fontSize = 12.sp, color = CatMapColors.TextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp), lineHeight = 16.sp)

        if (!addedFood) {
            ChipSection("Mama Kabı Durumu", "", listOf(ReportAction.OBSERVED_FOOD_FULL, ReportAction.OBSERVED_FOOD_EMPTY), selectedActions) { onIntent(SpotOperationIntent.ToggleAction(it)) }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (!addedWater) {
            ChipSection("Su Kabı Durumu", "", listOf(ReportAction.OBSERVED_WATER_FULL, ReportAction.OBSERVED_WATER_EMPTY), selectedActions) { onIntent(SpotOperationIntent.ToggleAction(it)) }
        }
    }
}

@Composable
fun Step3EnvironmentAndNotes(state: SpotOperationUiState, onIntent: (SpotOperationIntent) -> Unit) {
    Column {
        ChipSection(
            title = "Fiziksel Çevre (Hızlı Geçilebilir)",
            subtitle = "Etraf temiz miydi? Kaplarda hasar var mıydı? (Her şey normalse boş bırakabilirsin)",
            actions = listOf(ReportAction.ACTION_CLEANED, ReportAction.OBSERVED_DIRTY, ReportAction.ACTION_REPAIRED, ReportAction.OBSERVED_DAMAGED),
            selectedActions = state.selectedActions,
            onToggle = { onIntent(SpotOperationIntent.ToggleAction(it)) }
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Ekstra Notlar (İsteğe Bağlı)", "Gelen gönüllülere bırakmak istediğiniz bir mesaj var mı?")
        OutlinedTextField(
            value = state.note,
            onValueChange = { onIntent(SpotOperationIntent.UpdateNote(it)) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            placeholder = { Text("Örn: Sağdaki mama kabının altı delik...", color = CatMapColors.TextMuted, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CatMapColors.Accent, unfocusedBorderColor = CatMapColors.Divider, focusedContainerColor = CatMapColors.SurfaceWhite, unfocusedContainerColor = CatMapColors.SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )
    }
}

// ====================================================================================
// YARDIMCI BİLEŞENLER (COMPONENTS)
// ====================================================================================

// 🚀 YENİLİK: İlerleme Çizgileri (Progress Indicator)
@Composable
fun StepProgressIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalSteps) {
            val isSelected = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isSelected) CatMapColors.Accent else CatMapColors.Divider)
            )
        }
    }
}


@Composable
fun WizardFooter(currentStep: Int, text: String, isEnabled: Boolean, isUploading: Boolean, onBackClick: () -> Unit, onNextClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = CatMapColors.SurfaceWhite, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Eğer 2. veya 3. adımdaysak Geri Tuşunu Göster
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier.height(56.dp).weight(0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CatMapColors.Divider)
                ) {
                    Text("< Geri", fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary)
                }
            }

            // İlerleme / Gönder Tuşu
            Button(
                onClick = onNextClick, enabled = isEnabled && !isUploading,
                modifier = Modifier.height(56.dp).weight(if (currentStep > 1) 0.65f else 1f),
                colors = ButtonDefaults.buttonColors(containerColor = CatMapColors.Accent, disabledContainerColor = CatMapColors.Divider),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUploading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun PhotoRequirementCard(photoUri: android.net.Uri?, onCameraClick: () -> Unit, onRemovePhoto: () -> Unit) {
    if (photoUri != null) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).border(1.dp, CatMapColors.Divider, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(model = photoUri, contentDescription = "Çekilen Fotoğraf", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            IconButton(
                onClick = onRemovePhoto,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_close), contentDescription = "Fotoğrafı Sil", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(CatMapColors.Success.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Onaylı Kanıt", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onCameraClick() },
            color = CatMapColors.SurfaceWhite, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CatMapColors.Divider)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(CatMapColors.SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = null, tint = CatMapColors.TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Anlık Fotoğraf Ekle", fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary, fontSize = 14.sp)
                        Text("Zorunlu Adım", color = CatMapColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Icon(painterResource(id = R.drawable.ic_chevron_right), contentDescription = null, tint = CatMapColors.TextMuted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(title: String, subtitle: String, actions: List<ReportAction>, selectedActions: List<ReportAction>, onToggle: (ReportAction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary)
        if (subtitle.isNotEmpty()) {
            Text(text = subtitle, fontSize = 11.sp, color = CatMapColors.TextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp), lineHeight = 14.sp)
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            actions.forEach { action ->
                SingleChip(action = action, selectedActions = selectedActions, onToggle = onToggle)
            }
        }
    }
}

@Composable
private fun SingleChip(action: ReportAction, selectedActions: List<ReportAction>, onToggle: (ReportAction) -> Unit) {
    val isSelected = selectedActions.contains(action)
    val badge = action.toUiBadge()

    Surface(
        modifier = Modifier.clickable { onToggle(action) },
        color = if (isSelected) badge.containerColor else CatMapColors.SurfaceWhite,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) badge.borderColor else CatMapColors.Divider)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = badge.iconResId), contentDescription = null, tint = if (isSelected) badge.contentColor else CatMapColors.TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = badge.text, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) badge.contentColor else CatMapColors.TextSecondary)
        }
    }
}


@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary)
        Text(text = subtitle, fontSize = 12.sp, color = CatMapColors.TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
fun OperationTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CatMapColors.SurfaceWhite).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) { Icon(painterResource(id = R.drawable.ic_close), contentDescription = "Kapat", tint = CatMapColors.TextPrimary) }
        Text(text = "Durum Bildirimi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CatMapColors.TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
fun LockedTargetCard(spotName: String, distance: Int?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Seçilen Mama Noktası",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CatMapColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            color = CatMapColors.SurfaceWhite,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CatMapColors.Divider),
            shadowElevation = 2.dp // Hafif ve şık bir gölge
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol İkon: Daha modern, yuvarlatılmış kare (Rounded Rect) arka plan
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CatMapColors.Accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_puck),
                        contentDescription = null,
                        tint = CatMapColors.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Orta Metinler
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spotName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = distance.toSmartDistanceString(),
                        fontSize = 12.sp,
                        color = CatMapColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 🚀 YENİ: Profesyonel "Kilitli" Kapsülü (Status Pill)
                Surface(
                    color = CatMapColors.Success.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CatMapColors.Success.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kapsül içindeki minik durum noktası
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CatMapColors.Success, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kilitli",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatMapColors.Success
                        )
                    }
                }
            }
        }
    }
}