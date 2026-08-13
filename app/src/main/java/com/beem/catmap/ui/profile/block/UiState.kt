package com.beem.catmap.ui.profile.block
sealed class BlockActionState {
    object Idle : BlockActionState()
    data class Loading(val message: String = "İşlem yapılıyor...") : BlockActionState()
    data class Success(val message: String) : BlockActionState()
    data class Error(val message: String) : BlockActionState()
}