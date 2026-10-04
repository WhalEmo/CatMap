package com.beem.catmap.ui.upload

enum class UploadStage {
    IDLE,
    FETCHING_LOCATION,
    UPLOADING_ASSETS,
    ERROR,
    SUCCESS
}