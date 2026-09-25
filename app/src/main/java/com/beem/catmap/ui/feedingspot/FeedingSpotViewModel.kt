package com.beem.catmap.ui.feedingspot

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.SpotReport
import com.beem.catmap.data.repository.FeedingSpotRepository
import com.beem.catmap.ui.manager.FeedingSpotEventBus
import com.beem.catmap.ui.manager.FeedingSpotMapEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedingSpotViewModel : ViewModel() {

    private val repository = FeedingSpotRepository.getInstance()

    private val _feedingSpots = MutableStateFlow<List<FeedingSpot>>(emptyList())
    val feedingSpots = _feedingSpots.asStateFlow()

    private val _spotReports = MutableStateFlow<List<SpotReport>>(emptyList())
    val spotReports = _spotReports.asStateFlow()

    // 🚀 Yükleme ve Hata Durumları
    private val _uiState = MutableStateFlow<FeedingSpotUiState>(FeedingSpotUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // ⚡ Tek seferlik olaylar (Toast mesajı, Kamera kaydırma vb.)
    private val _eventFlow = MutableSharedFlow<FeedingSpotEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Fiziksel Yakınlık Kontrolü (50 Metre Kuralı)
    private val ALLOWED_RADIUS_METERS = 50f

    init {
        observeFeedingSpotEventsBus()
    }

    fun fetchReportsForSpot(spotId: String) {
        viewModelScope.launch {
            _uiState.value = FeedingSpotUiState.Loading("Geçmiş raporlar getiriliyor...")

            val result = repository.getReportsForSpot(spotId)

            if (result.isSuccess) {
                _spotReports.value = result.getOrDefault(emptyList())
                _uiState.value = FeedingSpotUiState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Raporlar alınamadı."
                _uiState.value = FeedingSpotUiState.Error(errorMsg)
                _eventFlow.emit(FeedingSpotEvent.ShowToast(errorMsg))
            }
        }
    }

    private fun addReport(report: SpotReport) {
        _spotReports.update { currentList ->
            if (!currentList.any { it.id == report.id }) {
                (listOf(report) + currentList).take(5)
            } else {
                currentList
            }
        }
    }

    private fun observeFeedingSpotEventsBus() {
        viewModelScope.launch {
            FeedingSpotEventBus.events.collect { event ->
                when (event) {
                    is FeedingSpotMapEvent.Created -> {
                       addReport(event.report)
                    }
                    is FeedingSpotMapEvent.Deleted -> {

                    }
                    is FeedingSpotMapEvent.Updated -> addReport(event.report)
                }
            }
        }
    }

}