package com.beem.catmap.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.ui.theme.PlusJakartaSans


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    appVersion: String,
    onAction: (ProfileSettingsAction) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ayarlar ve Hareketler",
                        fontFamily = PlusJakartaSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(ProfileSettingsAction.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = CatMapColors.TextPrimary
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CatMapColors.SurfaceWhite
                )
            )
        },
        containerColor = CatMapColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // GRUP 1: OYUNLAŞTIRMA VE PROFİL
            item {
                SettingsSectionTitle(title = "Kedi Haritası ve Başarımlar")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    SettingsItemRow(
                        title = "Rozetler ve Mahalle Başarımları",
                        subtitle = "Beslediğin kediler, kazandığın mahalle unvanları ve kademeli başarımlar",
                        iconRes = R.drawable.catmap_badge_tier_05,
                        iconTint = Color.Unspecified,
                        iconBg = CatMapColors.Badge.SoftCream,
                        badgePill = "Yeni",
                        badgePillColor = CatMapColors.Accent,
                        onClick = { onAction(ProfileSettingsAction.OpenBadges) }
                    )
                }
            }

            // GRUP 2: GİZLİLİK VE ETKİLEŞİM
            item {
                SettingsSectionTitle(title = "Gizlilik ve Güvenlik")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    SettingsItemRow(
                        title = "Engellenen Kullanıcılar",
                        subtitle = "Haritada ve profillerde etkileşimini kısıtladığın kişiler",
                        iconRes = R.drawable.exit,
                        iconTint = CatMapColors.Primary,
                        iconBg = CatMapColors.Divider.copy(alpha = 0.5f),
                        onClick = { onAction(ProfileSettingsAction.OpenBlockedUsers) }
                    )
                }
            }

            // GRUP 3: OTURUM VE ÇIKIŞ
            item {
                SettingsSectionTitle(title = "Giriş ve Oturum")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardContainer {
                    SettingsItemRow(
                        title = "Çıkış Yap",
                        subtitle = "Mevcut hesabından çıkış yap. Kaydedilen verilerin güvende kalır.",
                        iconRes = R.drawable.logout,
                        iconTint = CatMapColors.Error,
                        iconBg = CatMapColors.Error.copy(alpha = 0.1f),
                        isDestructive = true,
                        showChevron = false,
                        onClick = { onAction(ProfileSettingsAction.RequestSignOut) }
                    )
                }
            }

            // EN ALT: KURUMSAL SÜRÜM / FOOTER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CatMap",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sürüm $appVersion",
                        fontSize = 12.sp,
                        color = CatMapColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sokak dostlarımız için sevgiyle geliştirildi",
                        fontSize = 11.sp,
                        color = CatMapColors.TextMuted.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = CatMapColors.TextMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp)
    )
}

@Composable
private fun SettingsCardContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CatMapColors.SurfaceWhite,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CatMapColors.Divider,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SettingsItemRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    iconTint: Color,
    iconBg: Color,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    badgePill: String? = null,
    badgePillColor: Color = CatMapColors.Accent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = CatMapColors.SoftGray),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol İkon Kutusu
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Başlık, Rozet ve Açıklama Metinleri
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) CatMapColors.Error else CatMapColors.TextPrimary
                )

                if (badgePill != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgePillColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgePill,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CatMapColors.TextLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = CatMapColors.TextSecondary
            )
        }

        // Sağ Chevron İkonu
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CatMapColors.TextMuted.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}