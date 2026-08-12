package com.beem.catmap.data.model

import com.beem.catmap.ui.message.models.BlockState

data class PresenceAndBlockResult(
    val presenceState: PresenceState,
    val blockState: BlockState
)