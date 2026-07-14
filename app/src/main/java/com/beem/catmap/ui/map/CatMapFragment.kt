package com.beem.catmap.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.BottomSheetController
import com.beem.catmap.Maps.CatFactService
import com.beem.catmap.Maps.LocationEngine
import com.beem.catmap.Maps.MapKedi.Kediler
import com.beem.catmap.Maps.MapViewModel
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.Maps.TarananKediler
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentCatMapBinding
import com.beem.catmap.models.CatModel
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

class CatMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentCatMapBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var mapViewModel: MapViewModel? = null
    private var bottomSheetController: BottomSheetController? = null

    private val kediler = ArrayList<Kediler>()
    private val markerlar = ArrayList<Marker>()
    private val markerKEY = HashMap<String, Any?>()

    private var sonCekilenLat = 0.0
    private var sonCekilenLng = 0.0
    private var screenWidth = 0
    private var isPanelVisible = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapViewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels

        Log.d("CAT_MAP_FRAGMENT", "Kaptan: CatMap Fragment ayağa kalktı!")

        // 🎯 Oynat Bakalım: Tıklama dinleyicilerini ve LiveData gözlemcilerini erkenden bağlıyoruz usta!
        setupClickListeners()
        observeViewModel()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_actual_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d("CAT_MAP_FRAGMENT", "Kaptan: İç harita başarıyla ayağa kalktı ve hazır!")

        LocationEngine.startTracking(requireContext(), mMap!!)

        val catManager = TarananKediler()
        catManager.ButonGosterim(mMap!!, binding.root)

        catManager.Basildi(kediler, mMap!!, { resimlimarker() }, requireContext())

        mMap!!.setOnCameraIdleListener {
            catManager.ButonGosterim(mMap!!, binding.root)
        }

        mMap!!.setOnMarkerClickListener { marker ->
            if (marker.title != "konum") {
                val cat = findCat(marker.position)

                cat?.let {
                    if (activity is MapsActivity) {
                        (activity as MapsActivity).sonTiklananMarker = marker
                        (activity as MapsActivity).kedibilgisigetirme(it)
                    }
                }
            }
            true
        }

    }

    private fun findCat(location: LatLng): Kediler? {
        return kediler.firstOrNull { cat ->
            val epsilon = 0.000001
            Math.abs(cat.latitude - location.latitude) < epsilon &&
                    Math.abs(cat.longitude - location.longitude) < epsilon
        }
    }

    private fun setupClickListeners() {
        binding.fabCurrentLocation.setOnClickListener {
            if (mMap != null && sonCekilenLat != 0.0 && sonCekilenLng != 0.0) {
                val currentLatLng = LatLng(sonCekilenLat, sonCekilenLng)
                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
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

        // Paneli Kapat Butonu
        binding.btnClosePanel.setOnClickListener { hidePanel() }
    }

    private fun observeViewModel() {
        mapViewModel?.isLoading?.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.yuklemeekran.visibility = View.VISIBLE
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    _binding?.let {
                        binding.yuklemeekran.visibility = View.GONE
                        binding.btnShowFact.visibility = View.VISIBLE
                    }
                }, 500)
            }
        }

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

        // 🛡️ INSTANCE eklendi
        LocationEngine.fetchDataEvent.observe(viewLifecycleOwner) { event ->
            if (event != null && mapViewModel != null) {
                mapViewModel!!.fetchCatsNearLocation(event.latitude, event.longitude)
                sonCekilenLat = event.latitude
                sonCekilenLng = event.longitude
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel?.zoomToCatEvent?.collect { cat ->
                    kediler.add(
                        modelToKediler(cat)
                    )
                    buildMarker(cat)
                    focusOnCatOnMap(cat)
                }
            }
        }
    }


    private fun focusOnCatOnMap(cat: CatModel) {
        if (mMap == null) return
        val location = LatLng(cat.latitude, cat.longitude)
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))

    }

    private fun modelToKediler(model: CatModel): Kediler {
        return Kediler(
            model.id, model.kediAdi, model.kediHakkinda,
            model.latitude, model.longitude, model.mainPhotoUrl,
            ArrayList(model.photoUri), model.YukleyenKullaniciID
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