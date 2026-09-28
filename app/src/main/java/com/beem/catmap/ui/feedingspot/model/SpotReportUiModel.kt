package com.beem.catmap.ui.feedingspot.model

import com.beem.catmap.data.model.SpotReport

data class SpotReportUiModel(
    val report: SpotReport,
    val reporterPhotoUrl: String? = null,
    val reporterDisplayName: String = report.reporterName
)