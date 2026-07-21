package com.beem.catmap.Maps

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.models.CatModel
import com.beem.catmap.repository.CatRepository
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.map.LoadingState
import com.beem.catmap.ui.map.LoadingType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val repository = CatRepository()

    private val _catsList = MutableLiveData<List<CatModel>>()
    val catsList: LiveData<List<CatModel>> get() = _catsList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _zoomToCatEvent = MutableSharedFlow<CatModel>(replay = 0, extraBufferCapacity = 1)
    val zoomToCatEvent = _zoomToCatEvent.asSharedFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState = _loadingState.asStateFlow()

    private var lastFetchedLocation: Location? = null
    private val FETCH_THRESHOLD_METERS = 500f

    fun fetchAllCats() {
        if (_catsList.value != null && _catsList.value!!.isNotEmpty()) {
            return
        }
        viewModelScope.launch {

            try {
                val cats = repository.getAllCats()

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)
                } else {
                    _errorMessage.postValue("Haritada hiç kedi bulunamadı.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Bağlantı hatası: ${e.message}")
            } finally {
            }
        }
    }

    fun requestZoomToCat(catId: String) {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading(
                message = "Kedinin konum bilgileri alınıyor...",
                type = LoadingType.CAT_DETAIL
            )
            try {
                val cat = repository.findCatById(catId)
                cat?.let {
                    _zoomToCatEvent.emit(it)
                } ?: run {
                    UiMessageManager.emitMessage(UiMessageState.Error("Bu sevimli kedi artık haritada bulunmuyor."))
                }
            } catch (e: Exception) {
                UiMessageManager.emitMessage(UiMessageState.Error("Bağlantı hatası."))
            } finally {
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    fun checkAndFetchCatsIfMoved(newLat: Double, newLng: Double) {
        val newLocation = Location("GPS").apply {
            latitude = newLat
            longitude = newLng
        }

        val lastLoc = lastFetchedLocation

        if (lastLoc == null || lastLoc.distanceTo(newLocation) >= FETCH_THRESHOLD_METERS) {
            lastFetchedLocation = newLocation
            fetchCatsNearLocation(newLat, newLng)
        }
    }

    fun fetchCatsNearLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading(
                message = "Yakındaki patiler haritaya çağrılıyor...",
                type = LoadingType.MAP_FETCH
            )
            try {
                val cats = repository.getCatsNearLocation(latitude, longitude)

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)
                } else {
                    UiMessageManager.emitMessage(UiMessageState.Info("Yakınlarda hiç kedi taranmamış."))
                }
            } catch (e: Exception) {
                UiMessageManager.emitMessage(UiMessageState.Error("Harita yüklenemedi."))
            } finally {
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    fun scanCatsInArea(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading(
                message = "Çevredeki Kediler Taranıyor...",
                type = LoadingType.MAP_FETCH
            )

            try {
                val cats = repository.fetchCatsInArea(latitude, longitude)

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)

                    val centerLat = latitude
                    val centerLng = longitude

                    val closestCat = cats.minByOrNull { cat ->
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            centerLat, centerLng,
                            cat.latitude, cat.longitude,
                            results
                        )
                        results[0]
                    }

                    closestCat?.let {
                        _zoomToCatEvent.emit(it)
                    }

                    UiMessageManager.emitMessage(UiMessageState.Success("${cats.size} sevimli dostumuz bulundu!"))
                } else {
                    UiMessageManager.emitMessage(UiMessageState.Info("Bu yakınlarda henüz taranmış kedi bulunmuyor."))
                }

            } catch (e: Exception) {
                UiMessageManager.emitMessage(UiMessageState.Error("Tarama esnasında bir hata oluştu."))
            } finally {
                _loadingState.value = LoadingState.Idle
            }
        }
    }

}