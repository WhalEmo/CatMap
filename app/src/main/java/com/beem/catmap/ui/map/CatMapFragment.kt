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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.BottomSheetController
import com.beem.catmap.maps.CatFactService
import com.beem.catmap.maps.mapkedi.Kediler
import com.beem.catmap.maps.MapViewModel
import com.beem.catmap.maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentCatMapBinding
import com.beem.catmap.data.model.CatModel
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
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
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import com.beem.catmap.maps.LocationEngine
import com.beem.catmap.maps.LocationSettingsHandler
import com.beem.catmap.ui.markersclick.BottomSheetFragment
import com.beem.catmap.data.local.LocationCacheManager
import com.beem.catmap.engine.speedengine.MotionState
import com.beem.catmap.engine.speedengine.SpeedEngine
import com.beem.catmap.ui.markersclick.CatDetailViewModel
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import kotlinx.coroutines.Dispatchers

class CatMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCatMapBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mapViewModel: MapViewModel? = null
    private var bottomSheetController: BottomSheetController? = null

    private val catDetailViewModel: CatDetailViewModel by activityViewModels()

    private val kediler = ArrayList<Kediler>()
    private val markerlar = ArrayList<Marker>()
    private val markerKEY = HashMap<String, Any?>()

    private var lastGpsLocation: Location? = null
    private var screenWidth = 0
    private var isPanelVisible = false
    private var isTrackingUser = true

    private var lastScannedLocation: LatLng? = null

    private var myLocationMarker: Marker? = null

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
        screenWidth = displayMetrics.widthPixels

        Log.d("CAT_MAP_FRAGMENT", "Kaptan: CatMap Fragment ayağa kalktı!")

        setupClickListeners()
        setupCatMapText()
        observeViewModel()
        observeMotionState()
        renderSimpleUi()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_actual_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("CAT_MAP_FRAGMENT", "Kaptan: İç harita başarıyla ayağa kalktı ve hazır!")

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
                android.location.Location.distanceBetween(
                    sonMerkez.latitude, sonMerkez.longitude,
                    currentCenter.latitude, currentCenter.longitude,
                    results
                )

                if (results[0] > 500f) {
                    if (binding.btnScanArea.isGone) {
                        binding.btnScanArea.show()
                    }
                }
            }
        }


        mMap!!.setOnMarkerClickListener { marker ->
            if (marker.title != "konum") {
                //val cat = findCat(marker.position)
                val cat = marker.tag as? Kediler

                cat?.let {
                    if (activity is MapsActivity) {
                        (activity as MapsActivity).sonTiklananMarker = marker

                        val existingFragment =
                            childFragmentManager.findFragmentByTag(BottomSheetFragment.TAG)
                        if (existingFragment == null || !existingFragment.isAdded) {
                            val bottomSheet = BottomSheetFragment.newInstance(cat)
                            bottomSheet.show(childFragmentManager, BottomSheetFragment.TAG)
                        }
                    }
                }
            }
            true
        }
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

        binding.btnShowFact.setOnClickListener {
            if (!isPanelVisible) {
                CatFactService.getRandomCatFact(requireContext(), object : CatFactService.CatFactCallback {
                    override fun onSuccess(translatedFact: String) {
                        binding.tvCatFactSliding.text = translatedFact
                        binding.adView.loadAd(AdRequest.Builder().build())
                        showPanel()
                    }
                    override fun onError(errorMessage: String) {
                        binding.tvCatFactSliding.text = "Hata: $errorMessage"
                        showPanel()
                    }
                })
            } else {
                hidePanel()
            }
        }

        binding.btnScanArea.setOnClickListener {
            mMap?.let { map ->
                val currentCenter = map.cameraPosition.target

                binding.btnScanArea.hide()
                lastScannedLocation = currentCenter

                mapViewModel?.scanCatsInArea(currentCenter.latitude, currentCenter.longitude)
            }
        }

        binding.btnClosePanel.setOnClickListener { hidePanel() }
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
                for (model in catModels) {
                    val kedi = modelToKediler(model)
                    kediler.add(kedi)
                }
                resimlimarker()
            }
        }

        LocationEngine.fetchDataEvent.observe(viewLifecycleOwner) { event ->
            if (event != null && mapViewModel != null) {
                mapViewModel!!.checkAndFetchCatsIfMoved(event.latitude, event.longitude)

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
                    kediler.add(
                        modelToKediler(cat)
                    )
                    buildMarker(cat)
                    focusOnCatOnMap(cat)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel?.loadingState?.collect { state ->
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
                        removeCatMarkerFromMap(deletedCatId)
                    }
                }
            }
        }

    }

    private fun removeCatMarkerFromMap(catId: String) {
        val targetMarker = markerlar.firstOrNull { marker ->
            val cat = marker.tag as? Kediler
            cat?.id == catId
        }

        targetMarker?.let { marker ->
            marker.remove()
            markerlar.remove(marker)
        }

        kediler.removeAll { it.id == catId }
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

    private fun buildMarker(cat: CatModel) {
        if (activity == null || !isAdded) return
        requireActivity().runOnUiThread {
            if (markerKEY.containsKey(cat.mainPhotoUrl)) return@runOnUiThread
            markerKEY[cat.mainPhotoUrl] = null
            Glide.with(this)
                .asBitmap()
                .load(cat.mainPhotoUrl)
                .override(100, 100)
                .centerCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onLoadCleared(placeholder: Drawable?) {}
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        markerKEY.remove(cat.mainPhotoUrl)
                    }
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        if (mMap == null) return
                        val kedy = LatLng(cat.latitude, cat.longitude)
                        val customMarkerBitmap = fotoduzenle(resource)
                        val marker = mMap!!.addMarker(
                            MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                .position(kedy)
                                .title(cat.id)
                        )
                        marker?.tag = modelToKediler(cat)
                        marker?.let { markerlar.add(it) }
                    }
                })
        }
    }

    private fun resimlimarker() {
        if (activity == null || !isAdded) return
        requireActivity().runOnUiThread {
            for (kedi in kediler) {
                if (markerKEY.containsKey(kedi.url) || kedi.isMarkerOlustuMu) continue
                kedi.isMarkerOlustuMu = true
                markerKEY[kedi.url] = null

                Glide.with(this)
                    .asBitmap()
                    .load(kedi.url)
                    .override(100, 100)
                    .centerCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            kedi.isMarkerOlustuMu = false
                            markerKEY.remove(kedi.url)
                        }

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            if (mMap == null) return
                            val kedy = LatLng(kedi.latitude, kedi.longitude)
                            val customMarkerBitmap = fotoduzenle(resource)

                            val marker = mMap!!.addMarker(
                                MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(customMarkerBitmap))
                                    .position(kedy)
                                    .title(kedi.isim)
                            )
                            marker?.tag = kedi
                            marker?.let { markerlar.add(it) }
                        }
                    })
            }
        }
    }

    private fun fotoduzenle(imageBitmap: Bitmap): Bitmap {
        val markerView = LayoutInflater.from(requireContext()).inflate(R.layout.marker_tasarim, null)
        val markerImage = markerView.findViewById<CircleImageView>(R.id.marker_cat_image)
        markerImage.setImageBitmap(imageBitmap)

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

        val returnedBitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        markerView.draw(canvas)
        return returnedBitmap
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
                    .zIndex(999.0f)
            )
        } else {
            animateMarker(myLocationMarker!!, latLng)
        }
    }

    private fun setupCatMapText() {
        val orangeHex = String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(requireContext(), R.color.catmap_accent))
        val darkHex = String.format("#%06X", 0xFFFFFF and ContextCompat.getColor(requireContext(), R.color.catmap_text_dark))

        binding.tvBrandLogotype.text = HtmlCompat.fromHtml(
            "<font color='$orangeHex'>Cat</font><font color='$darkHex'>Map</font>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
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
                mMap?.let { LocationEngine.startTracking(requireContext(), it) }
            },
            onGpsDisabled = { exception ->
                showCatMapGpsDialog(exception as ResolvableApiException)
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

    private fun showPanel() {
        binding.rightSlidingPanel.animate().translationX(0f).setDuration(300).start()
        isPanelVisible = true
    }

    private fun hidePanel() {
        binding.rightSlidingPanel.animate().translationX(screenWidth.toFloat()).setDuration(300).start()
        isPanelVisible = false
    }

    fun handleSlidingPanelBackPress(): Boolean {
        if (isPanelVisible) {
            hidePanel()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocationEngine.stopTracking()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        bottomSheetController = null
    }
}