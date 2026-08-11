package com.beem.catmap.ui.message.models

data class MessageProfile(
    val name: String,
    val photoUrl: String,
    val blockState: BlockState = BlockState.None
)