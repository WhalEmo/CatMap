package com.beem.catmap.ui.message.models

sealed class BlockState {
    object None : BlockState()
    object BlockedByMe : BlockState()
    object BlockedByUser : BlockState()
    object MutualBlock : BlockState()
}