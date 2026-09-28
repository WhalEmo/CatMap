package com.beem.catmap.ui.map

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.BottomSheetController
import com.beem.catmap.maps.mapkedi.Kediler
import com.beem.catmap.maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentCatMapBinding
import com.beem.catmap.data.model.CatModel
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.HashMap
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import com.beem.catmap.engine.location.LocationEngine
import com.beem.catmap.maps.LocationSettingsHandler
import com.beem.catmap.ui.markersclick.BottomSheetFragment
import com.beem.catmap.data.local.LocationCacheManager
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.engine.speedengine.MotionState
import com.beem.catmap.engine.speedengine.SpeedEngine
import com.beem.catmap.ui.feedingspot.FeedingSpotBottomSheetFragment
import com.beem.catmap.ui.feedingspot.getCustomSpotMarker
import com.beem.catmap.ui.map.components.MapTopHeaderBar
import com.beem.catmap.ui.map.model.MapFilterType
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.spotoperation.SpotOperationFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CatMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCatMapBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mapViewModel: MapViewModel? = null
    private var bottomSheetController: BottomSheetController? = null


    private val activeCatMarkers = ConcurrentHashMap<String, Marker>()
    private val activeGlideTargets = ConcurrentHashMap<String, CustomTarget<Bitmap>>()

    private var cachedDefaultCatDescriptor: BitmapDescriptor? = null

    private val kediler = ArrayList<Kediler>()

    private var lastGpsLocation: Location? = null
    private var isTrackingUser = true

    private val spotMarkers = ArrayList<Marker>()

    private var lastScannedLocation: LatLng? = null

    private var myLocationMarker: Marker? = null

    private val isScanAreaVisible = mutableStateOf(false)
    private val isScanningArea = mutableStateOf(false)
    private val currentFilter = mutableStateOf(MapFilterType.ALL)
    private val catCountState = mutableIntStateOf(0)
    private val spotCountState = mutableIntStateOf(0)

    private val gpsEnablerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mMap?.let { LocationEngine.startTracking(requireContext(), it) }
        } else {
            UiMessageManager.emitMessage(UiMessageState.Info("Konum servisleri kapalı olduğu için harita güncellenemiyor."))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BottomSheetController) {
            bottomSheetController = context
        }
    }

    override fun onResume() {
        super.onResume()
        if (mMap != null && LocationEngine.hasLocationPermission(requireContext()) && LocationEngine.isGpsEnabled(requireContext())) {
            LocationEngine.startTracking(requireContext(), mMap!!)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleBackPressWithEngine()

        mapViewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

        Log.d("CAT_MAP_FRAGMENT", "Kaptan: CatMap Fragment ayağa kalktı!")

        setupComposeView()
        setupClickListeners()
        observeViewModel()
        observeMotionState()
        renderSimpleUi()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_actual_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("CAT_MAP_FRAGMENT", "Kaptan: İç harita başarıyla ayağa kalktı ve hazır!")

        mMap?.uiSettings?.isMapToolbarEnabled = false

        val cachedLocation = LocationCacheManager.getLastLocation()
        val cachedZoom = LocationCacheManager.getLastZoom()

        if (cachedLocation.latitude != 0.0 && cachedLocation.longitude != 0.0) {
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(cachedLocation, cachedZoom))
            updateMyLocationMarker(cachedLocation)
        }

        checkGpsAndStartTracking()

        lastScannedLocation = mMap!!.cameraPosition.target

        mMap!!.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                if (isTrackingUser) {
                    stopTrackingMode()
                }
            }
        }


        mMap!!.setOnCameraIdleListener {
            val currentCenter = googleMap.cameraPosition.target

            lastScannedLocation?.let { sonMerkez ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    sonMerkez.latitude, sonMerkez.longitude,
                    currentCenter.latitude, currentCenter.longitude,
                    results
                )

                if (results[0] > 500f) {
                    if (!isScanAreaVisible.value) {
                        isScanAreaVisible.value = true
                    }
                }
            }
        }


        mMap!!.setOnMarkerClickListener { marker ->
            if (marker == myLocationMarker || marker.title == "konum") {
                return@setOnMarkerClickListener true
            }

            val spot = marker.tag as? FeedingSpot
            if (spot != null) {
                val tag = "${FeedingSpotBottomSheetFragment.TAG}_${spot.id}"
                val existing = childFragmentManager.findFragmentByTag(tag)
                if (existing == null || !existing.isAdded) {
                    val bottomSheet = FeedingSpotBottomSheetFragment.newInstance(spot)
                    bottomSheet.show(childFragmentManager, tag)
                }
                return@setOnMarkerClickListener true
            }

            val cat = marker.tag as? Kediler
            if (cat != null) {
                if (activity is MapsActivity) {
                    (activity as MapsActivity).sonTiklananMarker = marker

                    val existingFragment = childFragmentManager.findFragmentByTag(BottomSheetFragment.TAG)
                    if (existingFragment == null || !existingFragment.isAdded) {
                        val bottomSheet = BottomSheetFragment.newInstance(cat)
                        bottomSheet.show(childFragmentManager, BottomSheetFragment.TAG)
                    }
                }
                return@setOnMarkerClickListener true
            }
            true
        }

        val currentSpots = mapViewModel?.feedingSpots?.value
        if (!currentSpots.isNullOrEmpty()) {
            renderFeedingSpotMarkers(currentSpots)
            spotCountState.intValue = currentSpots.size
        }
    }


    private fun setupComposeView() {
        binding.composeTopMapBar.setContent {
            MapTopHeaderBar(
                isScanAreaVisible = isScanAreaVisible.value,
                isScanning = isScanningArea.value,
                selectedFilter = currentFilter.value,
                catCount = catCountState.intValue,
                spotCount = spotCountState.intValue,
                onScanAreaClick = {
                    triggerAreaScan()
                },
                onFilterSelected = { filter ->
                    currentFilter.value = filter
                    applyMarkerVisibilityFilter(filter)
                }
            )
        }
    }

    private fun applyMarkerVisibilityFilter(filter: MapFilterType) {
        val showCats = filter == MapFilterType.ALL || filter == MapFilterType.CATS
        val showSpots = filter == MapFilterType.ALL || filter == MapFilterType.SPOTS

        activeCatMarkers.values.forEach { it.isVisible = showCats }
        spotMarkers.forEach { it.isVisible = showSpots }
    }

    private fun triggerAreaScan() {
        val map = mMap ?: return
        val currentCenter = map.cameraPosition.target

        isScanAreaVisible.value = false
        lastScannedLocation = currentCenter

        mapViewModel?.scanArea(currentCenter.latitude, currentCenter.longitude)
    }


    private fun setupClickListeners() {
        binding.fabCurrentLocation.setOnClickListener {
            if (!LocationEngine.isGpsEnabled(requireContext())) {
                checkGpsAndStartTracking()
                return@setOnClickListener
            }

            if (lastGpsLocation != null && mMap != null) {
                if (!isTrackingUser) {
                    startTrackingMode()
                } else {
                    stopTrackingMode()
                }
            } else {
                UiMessageManager.emitMessage(UiMessageState.Info("Konum aranıyor, lütfen bekleyin..."))
            }
        }

        checkAndShowTooltip()
    }

    private fun checkAndShowTooltip() {
        if (!UserSession.isAddSpotTooltipShown) {
            binding.tooltipAddSpot.visibility = View.VISIBLE

            // Kullanıcı butona VEYA baloncuğa tıkladığında baloncuğu sonsuza dek yok et
            val hideTooltip = {
                binding.tooltipAddSpot.visibility = View.GONE
                UserSession.isAddSpotTooltipShown = true
            }

            binding.tooltipAddSpot.setOnClickListener { hideTooltip() }
            binding.fabAddSpot.setOnClickListener {
                hideTooltip()
                openSpotOperationWizard()
            }
        } else {
            // Zaten görmüşse direkt tıklama olayını bağla
            binding.fabAddSpot.setOnClickListener {
                openSpotOperationWizard()
            }
        }
    }

    private fun openSpotOperationWizard() {
        val args = SpotOperationFragment.newArgs(
            spotId = "",
        )
        SmartNavigationEngine.navigateTo(
            Screen.SPOT_OPERATION,
            args,
        )
    }


    private fun observeMotionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SpeedEngine.motionState.collect { state ->
                    if (isTrackingUser && lastGpsLocation != null) {
                        updateCameraForTracking(lastGpsLocation!!, animate = true)
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        mapViewModel?.catsList?.observe(viewLifecycleOwner) { catModels ->
            if (catModels != null && catModels.isNotEmpty()) {
                kediler.clear()
                kediler.addAll(catModels.map { modelToKediler(it) })
                syncCatMarkersWithMap(kediler)
                catCountState.intValue = kediler.size
                applyMarkerVisibilityFilter(currentFilter.value)
            } else {
                catCountState.intValue = 0
            }
        }

        LocationEngine.fetchDataEvent.observe(viewLifecycleOwner) { event ->
            if (event != null && mapViewModel != null) {
                mapViewModel!!.checkAndFetchCatsIfMoved(event.latitude, event.longitude)

                mapViewModel!!.checkAndFetchSpotsIfMoved(event.latitude, event.longitude)

                lastGpsLocation = event

                val currentLatLng = LatLng(event.latitude, event.longitude)
                updateMyLocationMarker(currentLatLng)
                LocationCacheManager.saveLastLocation(LatLng(event.latitude, event.longitude))

                lifecycleScope.launch(Dispatchers.Default) {
                    SpeedEngine.processLocation(event)
                }

                if (isTrackingUser) {
                    updateCameraForTracking(event, animate = true)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel?.zoomToCatEvent?.collect { cat ->
                    stopTrackingMode()
                    val catModel = modelToKediler(cat)
                    if (!kediler.any { it.id == catModel.id }) {
                        kediler.add(catModel)
                    }
                    mMap?.let { renderSingleCatMarker(it, catModel) }
                    focusOnCatOnMap(cat)
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel?.feedingSpots?.collect { spots ->
                    renderFeedingSpotMarkers(spots)

                    spotCountState.intValue = spots.size
                    applyMarkerVisibilityFilter(currentFilter.value)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel?.loadingState?.collect { state ->
                    isScanningArea.value = (state is LoadingState.Loading)
                    when (state) {
                        is LoadingState.Idle -> {
                            binding.mapLoadingProgress.visibility = View.GONE
                            binding.loadingPill.animate().alpha(0f).setDuration(400).withEndAction {
                                binding.loadingPill.visibility = View.GONE
                            }.start()
                        }
                        is LoadingState.Loading -> {
                            if (state.type == LoadingType.MAP_FETCH) {
                                binding.mapLoadingProgress.visibility = View.VISIBLE
                            } else {
                                binding.loadingPill.apply {
                                    visibility = View.VISIBLE
                                    alpha = 0f
                                    animate().alpha(1f).setDuration(300).start()
                                }
                                binding.tvLoadingMessage.text = state.message
                            }
                        }
                    }
                }
            }
        }


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    mapViewModel?.deleteCatEvent?.collect { deletedCatId ->
                        cancelMarkerImageLoading(deletedCatId)
                        activeCatMarkers.remove(deletedCatId)?.remove()
                        kediler.removeAll { it.id == deletedCatId }
                    }
                }
            }
        }

    }


    private fun renderSimpleUi() {
        val fabCurrentLocationIcon = if(isTrackingUser) R.drawable.ic_location_puck_active else R.drawable.ic_location_puck
        binding.fabCurrentLocation.setImageResource(fabCurrentLocationIcon)
    }


    private fun focusOnCatOnMap(cat: CatModel) {
        if (mMap == null) return
        val location = LatLng(cat.latitude, cat.longitude)
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))

    }

    private fun modelToKediler(model: CatModel): Kediler {
        return Kediler(
            model.id,
            model.kediAdi,
            model.kediHakkinda,
            model.latitude,
            model.longitude,
            model.mainPhotoUrl,
            ArrayList(model.photoUri),
            model.YukleyenKullaniciID,
            model.createdAt,
            model.city,
            model.district,
            model.neighborhood
        )
    }

    private fun startTrackingMode() {
        isTrackingUser = true
        binding.fabCurrentLocation.setImageResource(R.drawable.ic_location_puck_active)

        lastGpsLocation?.let { loc ->
            updateCameraForTracking(loc, animate = true)
        }
    }

    private fun stopTrackingMode() {
        if (!isTrackingUser) return
        isTrackingUser = false
        binding.fabCurrentLocation.setImageResource(R.drawable.ic_location_puck)

        mMap?.let { map ->
            val currentPos = map.cameraPosition
            val newCamPos = CameraPosition.Builder(currentPos)
                .tilt(0f)
                .bearing(currentPos.bearing)
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(newCamPos), 500, null)
        }
    }

    private fun updateCameraForTracking(location: Location, animate: Boolean = true) {
        val map = mMap ?: return
        val currentLatLng = LatLng(location.latitude, location.longitude)
        val bearing = if (location.hasBearing() && location.bearing != 0f) {
            location.bearing
        } else {
            0f
        }

        val currentMotionState = SpeedEngine.motionState.value

        val currentBearing = if(currentMotionState == MotionState.STATIC) 0f else bearing

        val cameraPosition = CameraPosition.Builder()
            .target(currentLatLng)
            .zoom(currentMotionState.zoom)
            .tilt(currentMotionState.tilt)
            .bearing(currentBearing)
            .build()

        if (animate) {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800, null)
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun updateMyLocationMarker(latLng: LatLng) {
        val map = mMap ?: return

        if (myLocationMarker == null) {
            val puckIcon = getBitmapDescriptorFromVector(requireContext(), R.drawable.ic_location_puck)
            myLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .icon(puckIcon)
                    .anchor(0.5f, 0.5f)
                    .title("konum")
                    .zIndex(1.0f)
            )
        } else {
            animateMarker(myLocationMarker!!, latLng)
        }
    }

    private fun getOrCreateDefaultCatMarkerDescriptor(context: Context): BitmapDescriptor {
        cachedDefaultCatDescriptor?.let { return it }

        val markerView = LayoutInflater.from(context).inflate(R.layout.marker_tasarim, null)
        val markerImage = markerView.findViewById<CircleImageView>(R.id.marker_cat_image)
        markerImage.setImageResource(R.drawable.ic_cat)
        markerImage.circleBackgroundColor = ContextCompat.getColor(context, R.color.catmap_surface_translucent)

        val bitmap = createBitmapFromMeasuredView(markerView)
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        cachedDefaultCatDescriptor = descriptor
        return descriptor
    }

    private fun createBitmapFromMeasuredView(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap =
            createBitmap(view.measuredWidth.coerceAtLeast(1), view.measuredHeight.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun syncCatMarkersWithMap(cats: List<Kediler>) {
        val map = mMap ?: return
        if (!isAdded || context == null) return

        val currentCatIds = cats.map { it.id }.toSet()

        // Haritada olup yeni listede olmayan silinmiş kedi marker'larını temizle
        val iterator = activeCatMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentCatIds.contains(entry.key)) {
                cancelMarkerImageLoading(entry.key)
                entry.value.remove()
                iterator.remove()
            }
        }

        // Yeni veya güncellenen kedileri haritada göster
        for (cat in cats) {
            if (!activeCatMarkers.containsKey(cat.id)) {
                renderSingleCatMarker(map, cat)
            }
        }
    }

    private fun renderSingleCatMarker(map: GoogleMap, cat: Kediler) {
        val context = context ?: return
        val position = LatLng(cat.latitude, cat.longitude)

        val shouldBeVisible = currentFilter.value == MapFilterType.ALL || currentFilter.value == MapFilterType.CATS

        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .icon(getOrCreateDefaultCatMarkerDescriptor(context))
                .title(cat.isim)
                .zIndex(50.0f)
                .anchor(0.5f, 1.0f)
                .visible(shouldBeVisible)
        ) ?: return

        marker.tag = cat
        activeCatMarkers[cat.id] = marker

        // URL yoksa varsayılan pati ikonuyla kalmaya devam eder
        if (cat.url.isBlank()) return

        val markerPixelSize = 90

        val target = object : CustomTarget<Bitmap>(markerPixelSize, markerPixelSize) {
            override fun onLoadCleared(placeholder: Drawable?) {
                activeGlideTargets.remove(cat.id)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                activeGlideTargets.remove(cat.id)
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                activeGlideTargets.remove(cat.id)

                // Fragment görünürlüğü ve marker canlılığı kontrolü
                if (!isAdded || viewLifecycleOwner.lifecycle.currentState < Lifecycle.State.STARTED) return
                val activeMarker = activeCatMarkers[cat.id] ?: return

                // UI Thread'i kitlememek için layout ölçümleme ve çizimini Coroutine'e devret
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    val customBitmap = composeCircularMarkerBitmap(resource)

                    withContext(Dispatchers.Main) {
                        try {
                            activeMarker.setIcon(BitmapDescriptorFactory.fromBitmap(customBitmap))

                            val currentShouldBeVisible = currentFilter.value == MapFilterType.ALL || currentFilter.value == MapFilterType.CATS
                            activeMarker.isVisible = currentShouldBeVisible
                        } catch (e: Exception) {
                            Log.w("CatMapFragment", "Marker güncellenirken iptal edildi: ${e.message}")
                        }
                    }
                }
            }
        }

        activeGlideTargets[cat.id] = target

        Glide.with(this)
            .asBitmap()
            .load(cat.url)
            .override( markerPixelSize, markerPixelSize)
            .centerCrop()
            .into(target)
    }

    private fun composeCircularMarkerBitmap(imageBitmap: Bitmap): Bitmap {
        val context = context ?: return imageBitmap
        val markerView = LayoutInflater.from(context).inflate(R.layout.marker_tasarim, null)
        val markerImage = markerView.findViewById<CircleImageView>(R.id.marker_cat_image)
        markerImage.setImageBitmap(imageBitmap)

        return createBitmapFromMeasuredView(markerView)
    }

    /**
     * 6. Adım: Aktif Görsel İsteklerini İptal Etme (Memory Leak Kalkanı)
     */
    private fun cancelMarkerImageLoading(catId: String) {
        activeGlideTargets.remove(catId)?.let { target ->
            Glide.with(this).clear(target)
        }
    }

    /**
     * 7. Adım: Tüm Kedi Marker Sistemini Güvenli Sıfırlama
     */
    private fun clearAllCatMarkers() {
        activeGlideTargets.forEach { (_, target) ->
            Glide.with(this).clear(target)
        }
        activeGlideTargets.clear()

        activeCatMarkers.forEach { (_, marker) ->
            marker.remove()
        }
        activeCatMarkers.clear()
    }


    private fun renderFeedingSpotMarkers(spots: List<FeedingSpot>) {
        if (mMap == null) return

        spotMarkers.forEach { it.remove() }
        spotMarkers.clear()

        val shouldShowSpots = currentFilter.value == MapFilterType.ALL || currentFilter.value == MapFilterType.SPOTS
        // Yeni gelenleri tek tek haritaya bas
        for (spot in spots) {
            val location = spot.coordinates?.let { LatLng(it.latitude, it.longitude) } ?: continue

            // Renk ve ikon verisini senin Mapper'dan çekiyoruz
            val (colorRes, iconRes) = spot.currentStatus.getCustomSpotMarker()
            val stateColor = ContextCompat.getColor(requireContext(), colorRes)

            val markerView = LayoutInflater.from(context).inflate(R.layout.layout_spot_marker, null)
            val markerBg = markerView.findViewById<ImageView>(R.id.marker_bg)
            val markerStateIcon = markerView.findViewById<ImageView>(R.id.marker_state_icon)

            markerBg.setColorFilter(stateColor, android.graphics.PorterDuff.Mode.SRC_IN)

            markerStateIcon.setImageResource(iconRes)
            markerStateIcon.setColorFilter(stateColor, android.graphics.PorterDuff.Mode.SRC_IN)

            markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

            val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            markerView.draw(canvas)

            val marker = mMap!!.addMarker(
                MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .anchor(0.5f, 1.0f)
                    .zIndex(100.0f)
                    .visible(shouldShowSpots)
            )

            // Objeyi marker'ın içine göm (Tıklayınca BottomSheet açılsın diye)
            marker?.tag = spot
            marker?.let { spotMarkers.add(it) }
        }
    }

    private fun getBitmapDescriptorFromVector(
        context: Context,
        vectorResId: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val startPosition = marker.position
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 800
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            val v = animation.animatedFraction
            val lng = v * toPosition.longitude + (1 - v) * startPosition.longitude
            val lat = v * toPosition.latitude + (1 - v) * startPosition.latitude
            marker.position = LatLng(lat, lng)
        }
        valueAnimator.start()
    }

    private fun checkGpsAndStartTracking() {
        if (!LocationEngine.hasLocationPermission(requireContext())) {
            UiMessageManager.emitMessage(UiMessageState.Info("Lütfen konum iznini kontrol edin."))
            return
        }

        LocationSettingsHandler.checkLocationSettings(
            activity = requireActivity(),
            onGpsEnabled = {
                if (isAdded) {
                    mMap?.let { LocationEngine.startTracking(requireContext(), it) }
                }
            },
            onGpsDisabled = { exception ->
                if (isAdded) {
                    showCatMapGpsDialog(exception as ResolvableApiException)
                }
            }
        )
    }

    private fun showCatMapGpsDialog(resolvableException: ResolvableApiException) {
        if (isStateSaved) return

        val context = requireContext()

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("Konum Servisi Kapalı")
            .setMessage("Canlı harita takibi ve çevredeki kayıtları görüntülemek için konum servislerinin aktif olması gerekmektedir.")
            .setIcon(R.drawable.ic_location_puck)
            .setCancelable(true)
            .setPositiveButton("Konumu Etkinleştir") { dialog, _ ->
                dialog.dismiss()
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(resolvableException.resolution).build()
                    gpsEnablerLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    Log.e("CAT_MAP", "GPS Intent fırlatılamadı: ${e.message}")
                }
            }
            .setNegativeButton("Vazgeç") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        val backgroundDrawable = com.google.android.material.shape.MaterialShapeDrawable().apply {
            fillColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.catmap_surface_white)
            )
            setCornerSize(16f)
        }
        dialog.window?.setBackgroundDrawable(backgroundDrawable)

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(context, R.color.catmap_accent)
        )
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(context, R.color.catmap_text_muted)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearAllCatMarkers()
        LocationEngine.stopTracking()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        bottomSheetController = null
    }
}