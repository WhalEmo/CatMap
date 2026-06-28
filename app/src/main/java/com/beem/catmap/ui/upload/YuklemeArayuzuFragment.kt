package com.beem.catmap.ui.upload

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.CevrimIciYonetimi
import com.beem.catmap.MainActivity
import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.databinding.YuklemeArayuzuBinding
import com.beem.catmap.ui.camera.CameraFragment
import com.beem.catmap.ui.camera.FilmStripAdapter // Mevcut yatay şerit adaptörün dayıcım
import com.beem.catmap.ui.extensions.fadeIn
import com.beem.catmap.ui.extensions.fadeOut
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class YuklemeArayuzuFragment : Fragment() {

    private var _binding: YuklemeArayuzuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var messageManager: UyariMesaji

    private lateinit var photoAdapter: UploadPhotosAdapter

    private var interstitialAd: InterstitialAd? = null

    private var premiumDialog: PremiumUploadDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = YuklemeArayuzuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Temel Altyapı Kurulumları
        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        messageManager = UyariMesaji(requireContext(), false)

        setupRecyclerView()
        setupListeners()
        observeUiState()
        setupBackPressed()
        loadInterstitialAd()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSelectedPhotos.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

            photoAdapter = UploadPhotosAdapter()
            adapter = photoAdapter

            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 250
                removeDuration = 250
            }
        }
    }

    private fun setupListeners() {
        // Kaydetme Butonu (Mermer gibi validasyon ve tetikleme)
        binding.kaydetmeButonu.setOnClickListener {
            checkLocationPermissionAndUpload()
        }

        // Kamera Aç Butonu
        binding.kameraId.setOnClickListener {
            openCameraFragment()
        }

        // Galeriden Seç Butonu (Bizim o canavar alt sayfayı patlatır)
        binding.dosyaId.setOnClickListener {
            val gallerySheet = com.beem.catmap.ui.camera.GalleryBottomSheet()
            gallerySheet.show(parentFragmentManager, "GalleryBottomSheet")
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->

                // 1. Fotoğraf listesi takibi ve Placeholder yönetimi
                if (state.selectedImages.isNotEmpty()) {
                    binding.layoutPlaceholder.fadeOut(200)
                    binding.recyclerViewSelectedPhotos.fadeIn(250)
                    photoAdapter.updateList(state.selectedImages)
                } else {
                    binding.recyclerViewSelectedPhotos.fadeOut(200)
                    binding.layoutPlaceholder.fadeIn(250)
                }

                if (state.isLoading) {
                    if (premiumDialog == null) {
                        premiumDialog = PremiumUploadDialog(requireContext())
                        premiumDialog?.show()
                    }
                    premiumDialog?.updateProgress(state.uploadProgress)
                } else{
                    premiumDialog = null
                }

                // 3. Hata Durumu Yönetimi
                state.errorMessage?.let { errorMsg ->
                    messageManager.BasarisizDurum(errorMsg, 1500)
                }

                // 4. Başarı Durumu Yönetimi ve O Meşhur Onay Dialogu
                if (state.isSuccess && state.createdDocumentId != null) {
                    messageManager.BasariliDurum("Kedi haritaya başarıyla işlendi!", 1000)
                    showAdIfAvailable()
                    showPostSaveDialog(state.createdDocumentId)
                    viewModel.resetState() // Formu ve durumu güvenle sıfırla
                    clearFormFields()
                }
            }
        }
    }

    private fun checkLocationPermissionAndUpload() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
            return
        }

        messageManager.YuklemeDurum("Konum bilgisi alınıyor...")
        locationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null) {
                // Konum başarıyla alındığı an her şeyi ViewModel'e fırlatıyoruz dayıcım
                viewModel.uploadCat(
                    catName = binding.isimText.text.toString(),
                    catAbout = binding.hakkindaText.text.toString(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    userId = MainActivity.kullanici?.id ?: "" // Güvenli null kontrolü
                )
            } else {
                messageManager.BasarisizDurum("Cihazın konum bilgisi okunamadı!", 1500)
            }
        }.addOnFailureListener {
            messageManager.BasarisizDurum("Konum servisi hatası!", 1500)
        }
    }

    private fun showPostSaveDialog(docId: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_dialog_tasarimi, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CatMapDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btn_yes).setOnClickListener {
            messageManager.YuklemeDurum("Profiline ekleniyor...")
            com.beem.catmap.Profil.Gonderiler.GonderiKaydetmeYardimciSinif.kullaniciyaGonderiKaydet(
                requireActivity(), docId, binding.main, messageManager
            )
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_no).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun clearFormFields() {
        binding.isimText.text?.clear()
        binding.hakkindaText.text?.clear()
    }

    private fun openCameraFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CameraFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi()
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    // --- REKLAM MOTORU ALANI ---
    private fun loadInterstitialAd() {
        MobileAds.initialize(requireContext()) {}
        InterstitialAd.load(requireContext(), "ca-app-pub-3940256099942544/1033173712", AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            })
    }

    private fun showAdIfAvailable() {
        interstitialAd?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { interstitialAd = null }
                override fun onAdShowedFullScreenContent() { interstitialAd = null }
            }
            show(requireActivity())
        }
    }

    // --- YAŞAM DÖNGÜSÜ KONTROLLERİ (ÇEVRİMİÇİ TAKİBİ) ---
    override fun onResume() {
        super.onResume()
        CevrimIciYonetimi.getInstance().apply {
            setYuklemeEkraniGorunuyor(true)
            CevrimIciCalistir(MainActivity.kullanici)
        }
    }

    override fun onPause() {
        super.onPause()
        CevrimIciYonetimi.getInstance().apply {
            setYuklemeEkraniGorunuyor(false)
            CevrimIciCalistir(MainActivity.kullanici)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}