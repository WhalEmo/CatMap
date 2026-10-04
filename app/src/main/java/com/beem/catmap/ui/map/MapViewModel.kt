package com.beem.catmap.ui.map

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.CatModel
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.repository.FeedingSpotRepository
import com.beem.catmap.data.repository.MapRepository
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.FeedingSpotEventBus
import com.beem.catmap.ui.manager.FeedingSpotMapEvent
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.utils.CatLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {

    private val repository = MapRepository.getInstance()
    private val feedingSpotRepository = FeedingSpotRepository.getInstance()

    private val _catsList = MutableLiveData<List<CatModel>>()
    val catsList: LiveData<List<CatModel>> get() = _catsList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _zoomToCatEvent = MutableSharedFlow<CatModel>(replay = 0, extraBufferCapacity = 1)
    val zoomToCatEvent = _zoomToCatEvent.asSharedFlow()

    private val _deleteCatEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val deleteCatEvent = _deleteCatEvent.asSharedFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState = _loadingState.asStateFlow()

    private val _feedingSpots = MutableStateFlow<List<FeedingSpot>>(emptyList())
    val feedingSpots = _feedingSpots.asStateFlow()

    private var lastFetchedLocation: Location? = null
    private val FETCH_THRESHOLD_METERS = 500f

    private var lastFetchedSpotLocation: Location? = null
    private val SPOT_FETCH_THRESHOLD_METERS = 500f


    init {
        observeCatEvents()
        observeFeedingSpotEvents()
    }

    fun checkAndFetchSpotsIfMoved(newLat: Double, newLng: Double, force: Boolean = false) {
        val newLocation = Location("GPS").apply {
            latitude = newLat
            longitude = newLng
        }

        val lastLoc = lastFetchedSpotLocation

        if (force || lastLoc == null || lastLoc.distanceTo(newLocation) >= SPOT_FETCH_THRESHOLD_METERS) {
            lastFetchedSpotLocation = newLocation

            loadFeedingSpots(newLat, newLng, 3000.0)
        }
    }

    private fun loadFeedingSpots(lat: Double, lng: Double, radius: Double = 3000.0) {
        viewModelScope.launch {
            val result = feedingSpotRepository.getActiveSpotsNearLocation(
                lat = lat,
                lng = lng,
                radiusInMeters = radius
            )
            if (result.isSuccess) {
                val spots = result.getOrDefault(emptyList())
                _feedingSpots.value = spots
            } else {
                UiMessageManager.emitMessage(UiMessageState.Error("Mama noktaları yüklenemedi."))
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

    fun checkAndFetchCatsIfMoved(newLat: Double, newLng: Double, force: Boolean = false) {
        val newLocation = Location("GPS").apply {
            latitude = newLat
            longitude = newLng
        }

        val lastLoc = lastFetchedLocation

        if (force || lastLoc == null || lastLoc.distanceTo(newLocation) >= FETCH_THRESHOLD_METERS) {
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

    fun scanArea(latitude: Double, longitude: Double, radius: Double = 3000.0) {
        viewModelScope.launch {
            _loadingState.value = LoadingState.Loading(
                message = "Çevredeki Kediler Taranıyor...",
                type = LoadingType.MAP_FETCH
            )

            try {
                val spotsDeferred = async {
                    feedingSpotRepository.getActiveSpotsNearLocation(
                        lat = latitude,
                        lng = longitude,
                        radiusInMeters = radius
                    )
                }
                val catsDeferred = async {
                    repository.fetchCatsInArea(latitude, longitude)
                }

                val cats = catsDeferred.await()
                val spotsResult = spotsDeferred.await()

                val spots = if (spotsResult.isSuccess) spotsResult.getOrDefault(emptyList()) else emptyList()

                _feedingSpots.value = spots

                if (cats.isNotEmpty()) {
                    _catsList.postValue(cats)
                }

                val totalFound = cats.size + spots.size
                if (totalFound > 0) {
                    val message = when {
                        cats.isNotEmpty() && spots.isNotEmpty() ->
                            "${cats.size} kedi ve ${spots.size} mama noktası bulundu! 🐾"
                        cats.isNotEmpty() ->
                            "${cats.size} sevimli dostumuz bulundu!"
                        else ->
                            "${spots.size} mama noktası bulundu! 🥣"
                    }
                    UiMessageManager.emitMessage(UiMessageState.Success(message))
                } else {
                    UiMessageManager.emitMessage(UiMessageState.Info("Bu alanda henüz kayıtlı kedi veya mama noktası bulunmuyor."))
                }

            } catch (e: Exception) {
                CatLogger.logError("MapViewModel", "scanArea", e)
                UiMessageManager.emitMessage(UiMessageState.Error("Alan taranırken bir hata oluştu."))
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
                        Location.distanceBetween(
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


    private fun observeCatEvents() {
        viewModelScope.launch {
            CatEventBus.catMapEvent.collect { event ->
                val currentList = _catsList.value?.toMutableList() ?: mutableListOf()

                when (event) {
                    is CatMapEvent.Created -> {
                        if (!currentList.any { it.id == event.cat.id }) {
                            currentList.add(0, event.cat)

                            _catsList.postValue(currentList)
                        }

                        _zoomToCatEvent.emit(event.cat)
                    }

                    is CatMapEvent.Updated -> {
                        val index = currentList.indexOfFirst { it.id == event.cat.id }
                        if (index != -1) {
                            currentList[index] = event.cat
                            _catsList.postValue(currentList)
                        }
                    }

                    is CatMapEvent.Deleted -> {
                        val removed = currentList.removeAll { it.id == event.catId }
                        if (removed) {
                            _catsList.postValue(currentList)

                            _deleteCatEvent.emit(event.catId)
                        }
                    }
                }
            }
        }
    }

    private fun observeFeedingSpotEvents() {
        viewModelScope.launch {
            FeedingSpotEventBus.events.collect { event ->
                when (event) {
                    is FeedingSpotMapEvent.Created -> {
                        _feedingSpots.update { currentList ->
                            if (!currentList.any{ it.id == event.spot.id}) {
                                listOf(event.spot) + currentList
                            } else {
                                currentList
                            }
                        }
                    }
                    is FeedingSpotMapEvent.Deleted -> {

                    }
                    is FeedingSpotMapEvent.Updated -> {
                        _feedingSpots.update{ currentList ->
                            currentList.map { spot ->
                                if (spot.id == event.spot.id) {
                                    spot.copy(
                                        currentStatus = event.spot.currentStatus
                                    )
                                } else {
                                    spot
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}