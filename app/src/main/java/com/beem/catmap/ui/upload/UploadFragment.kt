package com.beem.catmap.ui.upload

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.engine.location.LocationEngine
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.local.location.LocationHelper
import com.beem.catmap.databinding.YuklemeArayuzuBinding
import com.beem.catmap.data.model.CatModel
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.camera.GalleryBottomSheet
import com.beem.catmap.ui.extensions.applyInputLimits
import com.beem.catmap.ui.extensions.fadeIn
import com.beem.catmap.ui.extensions.fadeOut
import com.beem.catmap.ui.manager.BadgeUnlockBus
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.manager.image.UploadSession
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.upload.components.UploadStatusBar
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UploadFragment : Fragment() {

    private var _binding: YuklemeArayuzuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels()
    private lateinit var locationClient: FusedLocationProviderClient

    private lateinit var photoAdapter: UploadPhotosAdapter

    private var premiumDialog: PremiumUploadDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = YuklemeArayuzuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        locationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        setupRecyclerView()
        setupListeners()
        observeUiState()
        setupBackPressed()
        setupComposeBanner()

        binding.hakkindaText.applyInputLimits(maxLength = 280, maxLines = 10)
        binding.isimText.applyInputLimits(maxLength = 20, maxLines = 2)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(!hidden) {
            if (!binding.main.isVisible) binding.main.visibility = View.VISIBLE
        }
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


    private fun setupComposeBanner() {
        binding.composeUploadBanner.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                UploadStatusBar(
                    stage = state.uploadStage,
                    progress = state.uploadProgress,
                    errorMessage = state.errorMessage,
                    onCancel = { viewModel.cancelUpload() },
                    onDismissError = { viewModel.dismissStatus() }
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        // Kaydetme Butonu (Mermer gibi validasyon ve tetikleme)
        binding.kaydetmeButonu.setOnClickListener {
            checkLocationPermissionAndUpload()
        }

        binding.kameraId.setOnClickListener {
            openCameraFragment()
        }

        binding.dosyaId.setOnClickListener {
            if (isAdded && isResumed) {
                val gallerySheet = GalleryBottomSheet.newInstance(UploadSession.GENERAL)
                gallerySheet.show(childFragmentManager, "GalleryBottomSheet")
            }
        }
        binding.hakkindaText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and android.view.MotionEvent.ACTION_MASK) == android.view.MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        binding.btnBack.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 1. Seçili fotoğraf listesi animasyonu
                    handlePhotoListVisibility(state.selectedImages)

                    // 2. Yükleme devam ederken kaydet butonunu kilitle (çift basımı engelle)
                    binding.kaydetmeButonu.isEnabled = !state.isLoading

                    // 3. Başarıyla tamamlandığında profil sorusu popup'ını aç
                    if (state.isSuccess && state.createdDocument != null) {
                        val cat = state.createdDocument
                        viewModel.resetState()
                        clearFormFields()
                        BadgeUnlockBus.setOnDismissAction {
                            if (isAdded && context != null) {
                                showPostSaveDialog(cat)
                            }
                        }
                    }
                }
            }
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun android.content.Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
    private fun handlePhotoListVisibility(images: List<Uri>) {
        if (images.isNotEmpty()) {
            binding.layoutPlaceholder.fadeOut(200)
            binding.recyclerViewSelectedPhotos.fadeIn(250)
            photoAdapter.updateList(images)
        } else {
            binding.recyclerViewSelectedPhotos.fadeOut(200)
            binding.layoutPlaceholder.fadeIn(250)
        }
    }




    private fun checkLocationPermissionAndUpload() {
        hideKeyboard()
        if(!LocationEngine.hasLocationPermission(requireContext())){
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 102)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val location = LocationEngine.getLastKnownLocation(requireContext())
            viewModel.uploadCat(
                catName = binding.isimText.text.toString(),
                catAbout = binding.hakkindaText.text.toString(),
                location = location,
                userId = UserSession.userId,
                locationHelper = LocationHelper(requireContext())
            )
        }
    }

    private fun showPostSaveDialog(cat: CatModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.alert_dialog_tasarimi, null)
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.CatMapDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<View>(R.id.btn_yes).setOnClickListener {

            viewModel.addCatPostMyProfile(cat.id) { isSuccess ->
                if (isSuccess) {

                    val newPost = Post(
                        catId = cat.id,
                        catName = cat.kediAdi,
                        bio = cat.kediHakkinda,
                        photoUrlList = cat.photoUri,
                        date = Timestamp.now(),
                        likeCount = 0L,
                        city = cat.city,
                        district = cat.district,
                        neighborhood = cat.neighborhood
                    )

                    viewLifecycleOwner.lifecycleScope.launch {
                        ProfileEventBus.emitEvent(ProfileEvent.PostAdded(newPost))
                    }

                    NavigationHelper.navigateToProfile(UserSession.userId)
                } else {
                    UiMessageManager.emitMessage(UiMessageState.Error("Profile ekleme başarısız oldu."))
                }
            }

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
        NavigationHelper.navigateToCamera(
            UploadSession.GENERAL
        )
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                SmartNavigationEngine.navigateBack()
            }
        })
    }

    override fun onDestroyView() {
        premiumDialog?.dismiss()
        premiumDialog = null
        super.onDestroyView()
        _binding = null
    }
}