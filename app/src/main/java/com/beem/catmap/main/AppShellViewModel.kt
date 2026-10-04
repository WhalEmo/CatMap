package com.beem.catmap.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.data.repository.BadgeRepository
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import kotlinx.coroutines.launch

class AppShellViewModel(application: Application) : AndroidViewModel(application) {

    private val badgeRepository = BadgeRepository.getInstance()

    fun onIntent(intent: AppShellIntent) {
        when (intent) {
            is AppShellIntent.EquipBadge -> equipBadge(intent.badgeModel)
        }
    }

    private fun equipBadge(badge: NeighborhoodBadgeModel) {
        val userId = UserSession.userId

        viewModelScope.launch {
            badgeRepository.equipBadge(userId, badge)
                .onSuccess {
                    UiMessageManager.emitMessage(
                        UiMessageState.Success("${badge.displayName} rozeti başarıyla kuşanıldı! 🎖️")
                    )
                }
                .onFailure { exception ->
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.message ?: "Rozet kuşanılırken bir hata oluştu.")
                    )
                }
        }
    }
}