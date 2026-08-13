package com.beem.catmap.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportViewModel() : ViewModel() {

    private val repository: ReportRepository = ReportRepository()

    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    fun submitReport(targetId: String, reportType: ReportType, reason: String) {
        if (targetId.isBlank() || reason.isBlank()) {
            _reportState.value = ReportUiState.Error("Geçersiz bildirim verisi.")
            return
        }

        viewModelScope.launch {
            _reportState.value = ReportUiState.Loading

            repository.sendReport(targetId, reportType, reason)
                .onSuccess {
                    _reportState.value = ReportUiState.Success
                }
                .onFailure { exception ->
                    _reportState.value = ReportUiState.Error(
                        exception.localizedMessage ?: "Firebase bağlantı hatası oluştu."
                    )
                }
        }
    }

    fun resetState() {
        _reportState.value = ReportUiState.Idle
    }
}