package com.beem.catmap.main

import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beem.catmap.ui.badge.celebration.BadgeCelebrationOverlay
import com.beem.catmap.ui.manager.BadgeUnlockBus

object GlobalOverlayBridge {

    @JvmStatic
    fun attachBadgeOverlay(
        composeView: ComposeView,
        onIntent: (AppShellIntent) -> Unit
    ) {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val celebrationPayload by BadgeUnlockBus.celebrationEvents
                    .collectAsStateWithLifecycle(initialValue = null)

                celebrationPayload?.let { payload ->
                    BadgeCelebrationOverlay(
                        payload = payload,
                        onEquipBadge = { badgeToEquip ->
                            onIntent(AppShellIntent.EquipBadge(badgeToEquip))
                            BadgeUnlockBus.dismiss()
                        },
                        onDismiss = {
                            BadgeUnlockBus.dismiss()
                        }
                    )
                }
            }
        }
    }
}