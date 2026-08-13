package com.beem.catmap.ui.report

sealed interface ReportUiState {
    object Idle : ReportUiState
    object Loading : ReportUiState
    object Success : ReportUiState
    data class Error(val message: String) : ReportUiState
}