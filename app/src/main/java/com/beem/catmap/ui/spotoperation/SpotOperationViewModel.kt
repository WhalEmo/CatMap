package com.beem.catmap.ui.spotoperation

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.CatMapApp
import com.beem.catmap.data.local.location.LocationHelper
import com.beem.catmap.data.model.CatAddressModel
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.data.repository.FeedingSpotRepository
import com.beem.catmap.engine.location.LocationAccessState
import com.beem.catmap.ui.manager.FeedingSpotEventBus
import com.beem.catmap.ui.manager.FeedingSpotMapEvent
import com.beem.catmap.ui.manager.image.ImageUploadManager
import com.beem.catmap.ui.manager.image.UploadSession
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.utils.CatLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpotOperationViewModel : ViewModel() {

    private val repository = FeedingSpotRepository.getInstance()

    private val _uiState = MutableStateFlow(SpotOperationUiState())
    val uiState: StateFlow<SpotOperationUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SpotOperationUiEvent>()
    val uiEvent: SharedFlow<SpotOperationUiEvent> = _uiEvent.asSharedFlow()


    private var uploadJob: Job? = null

    private var targetSpotId: String = ""

    private var targetLat: Double = 0.0
        get() = if (field == 0.0) {
            _uiState.value.userLocation?.latitude ?: 0.0
        } else {
            field
        }

    private var targetLng: Double = 0.0
        get() = if (field == 0.0) {
            _uiState.value.userLocation?.longitude ?: 0.0
        } else {
            field
        }

    private var lastFetchedLocation: Location? = null

    private val SHIELD_RADIUS_METERS = 50
    private val REFETCH_DISTANCE_THRESHOLD = 30f

    init {
        viewModelScope.launch {
            ImageUploadManager.observeSession(UploadSession.REPORT).collect { uris ->
                _uiState.update { state ->
                    val firstUri = uris.firstOrNull()
                    state.copy(
                        photoUri = firstUri,
                        isSubmitEnabled = checkValidationMatrix(state.selectedActions, firstUri != null, state.isCreateMode, state.newSpotName),
                        hasPhoto = firstUri != null
                    )
                }
            }
        }
    }

    fun onIntent(intent: SpotOperationIntent) {
        when (intent) {
            is SpotOperationIntent.Initialize -> handleInitialize(intent)
            is SpotOperationIntent.RetryShield -> restartGpsInitialize()
            is SpotOperationIntent.SelectSpot -> handleSpotSelection(intent.spotId)

            is SpotOperationIntent.ToggleAction -> toggleAction(intent.action)
            is SpotOperationIntent.UpdateNote -> updateNote(intent.note)
            is SpotOperationIntent.UpdateNewSpotName -> updateNewSpotName(intent.name)

            is SpotOperationIntent.NextStep -> handleNextStep()
            is SpotOperationIntent.PreviousStep -> handlePreviousStep()

            is SpotOperationIntent.SubmitReport -> submitReport()
            is SpotOperationIntent.OpenCamera -> NavigationHelper.navigateToCamera(UploadSession.REPORT)
            is SpotOperationIntent.NavigateBack -> SmartNavigationEngine.navigateBack()
            is SpotOperationIntent.RemovePhoto -> {
                _uiState.update { it.copy(hasPhoto = false) }
                ImageUploadManager.clearSession(UploadSession.REPORT)
            }

            is SpotOperationIntent.GpsError -> gpsError(intent.message)
            SpotOperationIntent.RestartGpsInitialize -> restartGpsInitialize()
            is SpotOperationIntent.RestartViewModel -> handleInitialize(intent.toInitial())
            is SpotOperationIntent.LocationUpdated -> handleLiveLocation(intent.location)
            SpotOperationIntent.BackHandler -> backHandler()
            SpotOperationIntent.UploadReportCancel -> cancelUploadManually()
            SpotOperationIntent.OpenAppSettingsClicked -> {
                viewModelScope.launch {
                    _uiEvent.emit(SpotOperationUiEvent.NavigateToAppSettings)
                }
            }
            SpotOperationIntent.RequestLocationPermissionClicked -> {
                viewModelScope.launch {
                    _uiEvent.emit(SpotOperationUiEvent.TriggerPermissionCoordinator)
                }
            }

            is SpotOperationIntent.UpdateLocationAccessState -> handleLocationAccessState(intent.state)
            SpotOperationIntent.ResolveGpsClicked -> {
                viewModelScope.launch {
                    _uiEvent.emit(SpotOperationUiEvent.ResolveGpsClicked)
                }
            }
        }
    }

    private fun gpsError(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(
                value = SpotOperationUiEvent.ShowToast(
                    message = message,
                    isSuccess = false
                )
            )
        }
    }

    private fun restartGpsInitialize() {
        _uiState.value.userLocation?.let {
            handleLiveLocation(it)
        }
    }

    // =========================================================================
    // 🚀 TEMİZLENMİŞ BAŞLATMA (INITIALIZE) MANTIĞI
    // =========================================================================
    private fun handleInitialize(intent: SpotOperationIntent.Initialize) {
        val isComingFromFab = intent.spotId.isEmpty()

        targetSpotId = intent.spotId
        targetLat = intent.lat
        targetLng = intent.lng


        val initialPreference = if (isComingFromFab) {
            SpotUserPreference.NewSpot
        } else {
            SpotUserPreference.SpecificSpot(intent.spotId)
        }

        _uiState.update {
            it.copy(
                isLockedMode = !isComingFromFab,
                isCreateMode = isComingFromFab,
                selectedSpotId = if (isComingFromFab) null else intent.spotId,
                spotName = if (isComingFromFab) "Yeni Nokta Keşfedildi" else intent.spotName,
                spotLocation = if (isComingFromFab) "Konum tespit ediliyor..." else "${intent.district}, ${intent.neighborhood}",
                currentStep = 1,
                selectedActions = emptyList(), // Formu temizle
                hasPhoto = false,
                userPreference = initialPreference,
                isLoadingRadar = true
            )
        }
    }

    private fun handleLiveLocation(location: Location) {
        val lastLoc = lastFetchedLocation
        val shouldRefetch = lastLoc == null || lastLoc.distanceTo(location) > REFETCH_DISTANCE_THRESHOLD

        viewModelScope.launch {
            if (shouldRefetch) {
                lastFetchedLocation = location
                refreshNearbySpotsFromRemote(location)
            } else {
                updateDistancesLocally(location)
            }
        }
    }

    private fun backHandler() {
        viewModelScope.launch {
            if (_uiState.value.isUploading) {
                _uiEvent.emit(
                    value = SpotOperationUiEvent.ShowToast(
                        message = "Yükleme işlemi yapılırken lütfen ekranda kalınız.",
                        isSuccess = false
                    )
                )
            }
        }
    }

    private fun handleLocationAccessState(accessState: LocationAccessState) {
        _uiState.update { current ->
            when (accessState) {
                LocationAccessState.READY -> {
                    val shouldShowLoading = current.userLocation == null && current.nearbySpots.isEmpty()
                    current.copy(
                        locationAccessState = accessState,
                        isGpsEnable = true,
                        isLoadingRadar = shouldShowLoading,
                        shieldState = if (current.shieldState == ShieldState.TOO_FAR) ShieldState.SCANNING else current.shieldState
                    )
                }
                LocationAccessState.GPS_DISABLED -> {
                    current.copy(
                        locationAccessState = accessState,
                        isGpsEnable = false,
                        isLoadingRadar = false,
                        shieldState = ShieldState.TOO_FAR,
                        isSubmitEnabled = false
                    )
                }
                LocationAccessState.PERMISSION_RATIONALE,
                LocationAccessState.PERMISSION_PERMANENT -> {
                    current.copy(
                        locationAccessState = accessState,
                        isGpsEnable = false,
                        isLoadingRadar = false,
                        shieldState = ShieldState.TOO_FAR,
                        isSubmitEnabled = false
                    )
                }
                LocationAccessState.CHECKING -> {
                    current.copy(locationAccessState = accessState)
                }
            }
        }

        // READY durumuna geçildiğinde hafızada son bilinen konum varsa hemen hesaplamayı başlat
        if (accessState == LocationAccessState.READY) {
            _uiState.value.userLocation?.let { loc ->
                handleLiveLocation(loc)
            }
        }
    }

    private fun cancelUploadManually() {
        if (_uiState.value.isUploading) {
            uploadJob?.cancel(CancellationException("İşlem isteğiniz üzerine iptal edildi."))
            uploadJob = null
        }
    }

    private suspend fun refreshNearbySpotsFromRemote(location: Location) {
        try {
            val distanceItems = fetchAndCalculateNearbySpots(location)
            applyLocationUpdate(location, distanceItems, isRemote = true)
        } catch (e: Exception) {
            _uiState.update { it.copy(shieldState = ShieldState.ERROR, isLoadingRadar = false) }
        }
    }

    /**
     * Gereksiz ağ isteklerini önler; sadece arayüzdeki mesafeleri gerçek zamanlı yeniden hesaplar.
     */
    private fun updateDistancesLocally(location: Location) {
        val updatedItems = _uiState.value.nearbySpots.map { item ->
            val spotLoc = Location("").apply {
                latitude = item.spot.coordinates?.latitude ?: 0.0
                longitude = item.spot.coordinates?.longitude ?: 0.0
            }
            item.copy(distance = spotLoc.distanceTo(location).toInt())
        }.sortedBy { it.distance }

        applyLocationUpdate(location, updatedItems, isRemote = false)
    }

    private fun applyLocationUpdate(
        location: Location,
        items: List<SpotDistanceItem>,
        isRemote: Boolean
    ) {
        val currentState = _uiState.value

        if (!currentState.isCreateMode && currentState.isUploading) {
            val currentSpotDistance = items.find { it.spot.id == targetSpotId }
            if (currentSpotDistance != null && currentSpotDistance.distance > SHIELD_RADIUS_METERS) {
                _uiState.update { state ->
                    state.copy(
                        userLocation = location,
                        nearbySpots = items,
                        shieldState = ShieldState.TOO_FAR,
                        isGpsEnable = true
                    )
                }
                uploadJob?.cancel(CancellationException("50 metre sınırından çıktığınız için işlem iptal edildi."))
                uploadJob = null
                return
            }
        }

        // 1. Kilitli moddaysa mod ve seçili nokta asla değiştirilemez
        if (currentState.isLockedMode) {
            _uiState.update { state ->
                state.copy(
                    userLocation = location,
                    nearbySpots = items,
                    isLoadingRadar = if (isRemote) false else state.isLoadingRadar,
                    isGpsEnable = true
                )
            }
            recalculateShieldForSelectedSpot()
            return
        }

        val closeSpot = items.firstOrNull { it.distance <= SHIELD_RADIUS_METERS }
        val isCreateAllowed = (closeSpot == null)

        val effectiveSelectedId: String?
        val effectiveCreateMode: Boolean

        when (val pref = currentState.userPreference) {
            SpotUserPreference.NewSpot -> {
                if (isCreateAllowed) {
                    effectiveSelectedId = null
                    effectiveCreateMode = true
                } else {
                    effectiveSelectedId = closeSpot.spot.id
                    effectiveCreateMode = false
                }
            }
            is SpotUserPreference.SpecificSpot -> {
                val isTargetStillAround = items.any{ it.spot.id == pref.spotId }
                if (isTargetStillAround) {
                    effectiveSelectedId = pref.spotId
                    effectiveCreateMode = false
                } else {
                    effectiveSelectedId = if (isCreateAllowed) null else closeSpot.spot.id
                    effectiveCreateMode = isCreateAllowed
                }
            }
        }


        _uiState.update { state ->
            state.copy(
                userLocation = location,
                nearbySpots = items,
                isCreateMode = effectiveCreateMode,
                isCreateAllowed = isCreateAllowed,
                selectedSpotId = effectiveSelectedId,
                isLoadingRadar = if (isRemote) false else state.isLoadingRadar,
                isGpsEnable = true
            )
        }
        recalculateShieldForSelectedSpot()
    }


    private fun logLatLng() {
        Log.d("SPOT_DEBUG", "$targetSpotId - ($targetLat, $targetLng)")
    }


    private suspend fun fetchAndCalculateNearbySpots(
        location: Location
    ): List<SpotDistanceItem> {
        val result = repository.getActiveSpotsNearLocation(
            lat = location.latitude,
            lng = location.longitude,
            radiusInMeters = 3000.0
        )
        val spots = result.getOrDefault(emptyList())

        return spots.map { spot ->
            val spotLocation = Location("").apply {
                latitude = spot.coordinates?.latitude ?: 0.0
                longitude = spot.coordinates?.longitude ?: 0.0
            }
            SpotDistanceItem(
                spot = spot,
                distance = spotLocation.distanceTo(location).toInt()
            )
        }.sortedBy { it.distance }
    }

    private suspend fun findLocationAddress(latitude: Double, longitude: Double): CatAddressModel {
        return withContext(Dispatchers.IO) {
            try {
                val address = LocationHelper(CatMapApp.instance).getFormattedAddress(latitude, longitude)
                address ?: CatAddressModel(
                    city = "Bilinmeyen İl",
                    district = "Bilinmeyen İlçe",
                    neighborhood = "Bilinmeyen Mahalle",
                    fullAddress = ""
                )
            } catch (e: Exception) {
                CatLogger.logError("SpotOperationVM", "findLocationAddress", e)
                CatAddressModel(
                    city = "Bilinmeyen İl",
                    district = "Bilinmeyen İlçe",
                    neighborhood = "Bilinmeyen Mahalle",
                    fullAddress = ""
                )
            }
        }
    }


    private fun log(key: String, value: String, tag: String = "SPOT_DEBUG"){
        Log.d(tag, "$key - $value")
    }

    // =========================================================================
    // 🚀 TEMİZLENMİŞ SEÇİM MANTIĞI (Sadece Radar Modunda Çalışır)
    // =========================================================================
    private fun handleSpotSelection(spotId: String?) {

        val newPreference = if (spotId == null) {
            SpotUserPreference.NewSpot
        } else {
            SpotUserPreference.SpecificSpot(spotId)
        }

        _uiState.update { it.copy(userPreference = newPreference) }

        _uiState.value.userLocation?.let { loc ->
            applyLocationUpdate(
                location = loc,
                items = _uiState.value.nearbySpots,
                isRemote = false
            )
        }
    }

    private fun recalculateShieldForSelectedSpot() {
        val state = _uiState.value

        if (state.selectedSpotId == null) {

            val hasTargetChanged = !state.isCreateMode || targetSpotId.isNotEmpty()
            targetSpotId = ""

            _uiState.update {
                it.copy(
                    isCreateMode = true,
                    shieldState = ShieldState.VERIFIED, // Bulunduğun yer otomatik olarak onaylıdır
                    distanceInMeters = 0,
                    spotName = "Yeni Nokta Keşfedildi",
                    currentStep = if (hasTargetChanged) 1 else it.currentStep,
                    isSubmitEnabled = checkValidationMatrix(it.selectedActions, it.hasPhoto, true, it.newSpotName)
                )
            }
        } else {
            // VAR OLAN BİR NOKTA SEÇİLDİ (Kilitli veya Radar üzerinden)
            val selectedItem = state.nearbySpots.find { it.spot.id == state.selectedSpotId }

            log("spotId", state.selectedSpotId)

            if (selectedItem != null) {
                val distance = selectedItem.distance
                log("distance", value = distance.toString())
                val isVerified = distance <= SHIELD_RADIUS_METERS
                log("isVerified", isVerified.toString())

                val hasTargetChanged = targetSpotId != selectedItem.spot.id || state.isCreateMode
                targetSpotId = selectedItem.spot.id

                _uiState.update {
                    it.copy(
                        isCreateMode = false,
                        shieldState = if (isVerified) ShieldState.VERIFIED else ShieldState.TOO_FAR,
                        distanceInMeters = distance,
                        spotName = selectedItem.spot.spotName,
                        spotLocation = "${selectedItem.spot.district}, ${selectedItem.spot.neighborhood}",
                        currentStep = if (hasTargetChanged) 1 else it.currentStep,
                        isSubmitEnabled = checkValidationMatrix(it.selectedActions, it.hasPhoto, false, it.newSpotName)
                    )
                }
            }
        }
    }

    // =========================================================================
    // İLERİ / GERİ ADIM MANTIĞI
    // =========================================================================
    private fun handleNextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                if (state.selectedActions.contains(ReportAction.ACTION_ADDED_BOTH)) {
                    _uiState.update { it.copy(currentStep = 3) }
                } else {
                    _uiState.update { it.copy(currentStep = 2) }
                }
            }
            2 -> _uiState.update { it.copy(currentStep = 3) }
        }
    }

    private fun handlePreviousStep() {
        val state = _uiState.value
        when (state.currentStep) {
            2 -> _uiState.update { it.copy(currentStep = 1) }
            3 -> {
                if (state.selectedActions.contains(ReportAction.ACTION_ADDED_BOTH)) {
                    _uiState.update { it.copy(currentStep = 1) }
                } else {
                    _uiState.update { it.copy(currentStep = 2) }
                }
            }
        }
    }

    private fun updateNewSpotName(newName: String) {
        _uiState.update { it.copy(newSpotName = newName, isSubmitEnabled = checkValidationMatrix(it.selectedActions, it.hasPhoto, it.isCreateMode, newName)) }
    }


    /**
     * Akıllı Etiket (Chip) Seçimi
     */
    fun toggleAction(action: ReportAction) {
        _uiState.update { state ->
            val currentList = state.selectedActions.toMutableList()

            if (currentList.contains(action)) {
                currentList.remove(action)
            } else {
                when (action) {
                    // --- İKMAL ÇAKIŞMALARI (UI'dan gizlenenleri listeden de siliyoruz) ---
                    ReportAction.ACTION_ADDED_BOTH -> {
                        currentList.removeAll(listOf(
                            ReportAction.ACTION_ADDED_FOOD, ReportAction.ACTION_ADDED_WATER,
                            ReportAction.OBSERVED_FOOD_FULL, ReportAction.OBSERVED_FOOD_EMPTY,
                            ReportAction.OBSERVED_WATER_FULL, ReportAction.OBSERVED_WATER_EMPTY
                        ))
                    }
                    ReportAction.ACTION_ADDED_FOOD -> {
                        currentList.removeAll(listOf(
                            ReportAction.ACTION_ADDED_BOTH,
                            ReportAction.OBSERVED_FOOD_FULL, ReportAction.OBSERVED_FOOD_EMPTY
                        ))
                    }
                    ReportAction.ACTION_ADDED_WATER -> {
                        currentList.removeAll(listOf(
                            ReportAction.ACTION_ADDED_BOTH,
                            ReportAction.OBSERVED_WATER_FULL, ReportAction.OBSERVED_WATER_EMPTY
                        ))
                    }

                    // --- GÖZLEM ÇAKIŞMALARI (Kullanıcı Gözlem seçerse, İkmal yapmış olamaz) ---
                    ReportAction.OBSERVED_FOOD_FULL, ReportAction.OBSERVED_FOOD_EMPTY -> {
                        currentList.removeAll(listOf(ReportAction.ACTION_ADDED_BOTH, ReportAction.ACTION_ADDED_FOOD))
                        if (action == ReportAction.OBSERVED_FOOD_FULL) currentList.remove(ReportAction.OBSERVED_FOOD_EMPTY)
                        else currentList.remove(ReportAction.OBSERVED_FOOD_FULL)
                    }
                    ReportAction.OBSERVED_WATER_FULL, ReportAction.OBSERVED_WATER_EMPTY -> {
                        currentList.removeAll(listOf(ReportAction.ACTION_ADDED_BOTH, ReportAction.ACTION_ADDED_WATER))
                        if (action == ReportAction.OBSERVED_WATER_FULL) currentList.remove(ReportAction.OBSERVED_WATER_EMPTY)
                        else currentList.remove(ReportAction.OBSERVED_WATER_FULL)
                    }

                    // --- FİZİKSEL DURUM ÇAKIŞMALARI ---
                    ReportAction.ACTION_CLEANED -> currentList.remove(ReportAction.OBSERVED_DIRTY)
                    ReportAction.OBSERVED_DIRTY -> currentList.remove(ReportAction.ACTION_CLEANED)
                    ReportAction.ACTION_REPAIRED -> currentList.remove(ReportAction.OBSERVED_DAMAGED)
                    ReportAction.OBSERVED_DAMAGED -> currentList.remove(ReportAction.ACTION_REPAIRED)

                    else -> {}
                }

                // Zıtlıklar temizlendikten sonra yeniyi ekle
                currentList.add(action)
            }

            // Seçim her değiştiğinde doğrulama matrisini tekrar çalıştır
            state.copy(
                selectedActions = currentList,
                isSubmitEnabled = checkValidationMatrix(currentList, state.hasPhoto, state.isCreateMode, state.newSpotName)
            )
        }
    }

    fun updateNote(newNote: String) {
        _uiState.update { it.copy(note = newNote) }
    }

    /**
     * Akıllı Filtre Matrisi (Faz 2)
     * Dönüş değeri true ise "Gönder" butonu aktif olur.
     */
    private fun checkValidationMatrix(actions: List<ReportAction>, hasPhoto: Boolean, isCreateMode: Boolean, spotName: String): Boolean {
        if (!hasPhoto) return false
        if (actions.isEmpty()) return false
        if (targetLng == 0.0 && targetLat == 0.0) return false
        if (isCreateMode && spotName.trim().isEmpty()) return false
        return true
    }

    /**
     * Veritabanına Yazma (Faz 3) - Temiz ve SRP'ye Uygun
     */
    fun submitReport() {
        val state = _uiState.value
        if (state.isUploading) return


        val currentUserLoc = state.userLocation
        if (currentUserLoc == null) {
            viewModelScope.launch {
                _uiEvent.emit(
                    SpotOperationUiEvent.ShowToast(
                        message = "Konum bilginiz henüz alınamadı. Lütfen bekleyin.",
                        isSuccess = false
                    )
                )
            }
            return
        }

        val finalSpotName = if (state.isCreateMode) {
            state.newSpotName.trim()
        } else {
            state.spotName
        }

        val validationError = getValidationError(
            actions = state.selectedActions,
            hasPhoto = state.hasPhoto,
            isCreateMode = state.isCreateMode,
            spotName = state.newSpotName
        )

        if (validationError != null) {
            viewModelScope.launch {
                _uiEvent.emit(SpotOperationUiEvent.ShowToast(validationError, isSuccess = false))
            }
            return
        }

        _uiState.update { it.copy(isUploading = true) }

        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            try {
                val uri = state.photoUri ?: throw Exception("Fotoğraf bulunamadı!")
                val repo = FeedingSpotRepository.getInstance()

                // Sadece iş mantığını (Business Logic) çöz (Hangi duruma geçti?)
                val newSpotState = determineNewSpotState(state.selectedActions)

                val addressModel = if (state.isCreateMode) findLocationAddress(targetLat, targetLng) else CatAddressModel(state.city, state.district, state.neighborhood, "")

                // Bütün hamallığı Repository'ye devret
                coroutineContext.ensureActive()
                val result = repo.submitSpotOperation(
                    isCreateMode = state.isCreateMode,
                    existingSpotId = targetSpotId,
                    lat = targetLat,
                    lng = targetLng,
                    spotName = finalSpotName,
                    city = addressModel.city,
                    district = addressModel.district,
                    neighborhood = addressModel.neighborhood,
                    photoUri = uri,
                    reportTags = state.selectedActions.map { it.name },
                    note = state.note.trim(),
                    newStatus = newSpotState,
                    userLat = currentUserLoc.latitude,
                    userLng = currentUserLoc.longitude
                )

                coroutineContext.ensureActive()

                if (result.isSuccess) {
                    val msg = if (state.isCreateMode) "Harika! Yeni nokta topluluğa kazandırıldı." else "Güncelleme başarıyla paylaşıldı."
                    _uiEvent.emit(SpotOperationUiEvent.ShowToast(msg, true))

                    val data = result.getOrThrow()

                    if (state.isCreateMode) {
                        FeedingSpotEventBus.emitEvent(
                            FeedingSpotMapEvent.Created(
                                spot = data.spot,
                                report = data.report
                            )
                        )
                    } else {
                        FeedingSpotEventBus.emitEvent(
                            FeedingSpotMapEvent.Updated(
                                spot = data.spot,
                                report = data.report
                            )
                        )
                    }

                    ImageUploadManager.clearSession(UploadSession.REPORT)
                    _uiState.update { it.copy(hasPhoto = false, isUploading = false) }
                    _uiState.update { currentState ->
                        currentState.copy(
                            newSpotName = "",
                            isUploading = false,
                            hasPhoto = false,
                            photoUri = null,
                            nearbySpots = currentState.nearbySpots + data.spot.toSpotDistanceItem(currentState.userLocation),
                            note = "",
                            selectedActions = emptyList(),
                        )
                    }
                    _uiEvent.emit(SpotOperationUiEvent.NavigateBack)
                } else {
                    throw result.exceptionOrNull() ?: Exception("Veritabanı kayıt hatası.")
                }

            } catch (e: CancellationException) {
                Log.w("SPOT_DEBUG2", "Yükleme iptal edildi: ${e.message}")

                _uiState.update { it.copy(isUploading = false) }

                recalculateShieldForSelectedSpot()

                val rawMessage = e.message ?: ""
                val userFriendlyReason = when {
                    rawMessage.contains("isteğiniz üzerine") -> "İşlem isteğiniz üzerine iptal edildi."
                    rawMessage.contains("50 metre") -> "Noktadan 50 metre uzaklaştığınız için işlem güvenlik amacıyla iptal edildi."

                    else -> "İşlem tamamlanamadan sonlandırıldı."
                }

                withContext(NonCancellable) {
                    _uiEvent.emit(
                        SpotOperationUiEvent.ShowToast(
                            message = userFriendlyReason,
                            isSuccess = false
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploading = false) }
                recalculateShieldForSelectedSpot()
                val errorMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true -> "İnternet bağlantısı kesildi. Lütfen tekrar deneyin."
                    e.message?.contains("Fotoğraf") == true -> "Fotoğraf yüklenirken bir sorun oluştu."
                    else -> "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin."
                }
                _uiEvent.emit(SpotOperationUiEvent.ShowToast(errorMessage, false))
            }
        }
    }

    private fun getValidationError(
        actions: List<ReportAction>,
        hasPhoto: Boolean,
        isCreateMode: Boolean,
        spotName: String
    ): String? {
        return when {
            actions.isEmpty() -> "Lütfen en az bir işlem veya durum seçin."
            targetLat == 0.0 && targetLng == 0.0 -> "Konum bilgisi alınamadı. Lütfen GPS bağlantınızı kontrol edin."
            !hasPhoto -> "Lütfen önce noktanın bir fotoğrafını ekleyin."
            isCreateMode && spotName.trim().isEmpty() -> "Lütfen besleme noktası için bir isim girin."
            else -> null
        }
    }

    private fun determineNewSpotState(actions: List<ReportAction>): com.beem.catmap.data.model.SpotState {

        // 🚀 1. KURAL: FİZİKSEL ÇEVRE (ÖNCELİKLİ DURUM)
        // Eğer kaplar kırık veya aşırı kirliyse, mama dolu olsa bile bakıma ihtiyaç vardır!
        val needsMaintenance = actions.contains(ReportAction.OBSERVED_DIRTY) ||
                actions.contains(ReportAction.OBSERVED_DAMAGED)

        if (needsMaintenance) {
            return com.beem.catmap.data.model.SpotState.NEEDS_MAINTENANCE
        }

        // 🚀 2. KURAL: MAMA VE SU HESAPLAMASI
        val addedBoth = actions.contains(ReportAction.ACTION_ADDED_BOTH)
        val addedFood = actions.contains(ReportAction.ACTION_ADDED_FOOD) || addedBoth
        val addedWater = actions.contains(ReportAction.ACTION_ADDED_WATER) || addedBoth

        val observedFoodFull = actions.contains(ReportAction.OBSERVED_FOOD_FULL)
        val observedWaterFull = actions.contains(ReportAction.OBSERVED_WATER_FULL)

        // Bir kabın dolu sayılması için ya ikmal yapılmış olmalı ya da dolu olduğu gözlemlenmeli
        val isFoodFull = addedFood || observedFoodFull
        val isWaterFull = addedWater || observedWaterFull

        // Senin Enum (SpotState) karşılıklarıyla birebir eşleştirme:
        return when {
            isFoodFull && isWaterFull -> com.beem.catmap.data.model.SpotState.FULL
            isFoodFull && !isWaterFull -> com.beem.catmap.data.model.SpotState.NEEDS_WATER
            !isFoodFull && isWaterFull -> com.beem.catmap.data.model.SpotState.NEEDS_FOOD
            else -> com.beem.catmap.data.model.SpotState.NEEDS_BOTH
        }
    }

    private fun FeedingSpot.toSpotDistanceItem(userLocation: Location?): SpotDistanceItem {
        val feedingSpot = this

        val nonNullLocation = requireNotNull(userLocation) {
            "Kullanıcı konumuna erişilemedi."
        }

        val location = Location("").apply {
            latitude = feedingSpot.coordinates?.latitude ?: 0.0
            longitude = feedingSpot.coordinates?.longitude ?: 0.0
        }
        return SpotDistanceItem(
            spot = feedingSpot,
            distance = location.distanceTo(nonNullLocation).toInt()
        )
    }


    private fun SpotOperationIntent.RestartViewModel.toInitial(): SpotOperationIntent.Initialize {
        return SpotOperationIntent.Initialize(
            spotId,
            lat,
            lng,
            spotName,
            district,
            neighborhood
        )
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel ölürken bellek sızıntısını önlemek için oturumu temizle
        ImageUploadManager.clearSession(UploadSession.REPORT)
    }
}