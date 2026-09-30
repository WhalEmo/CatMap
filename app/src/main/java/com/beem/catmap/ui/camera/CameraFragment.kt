package com.beem.catmap.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentCameraBinding
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.fragment.app.DialogFragment
import com.beem.catmap.ui.manager.image.UploadSession
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine

class CameraFragment : DialogFragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var lensSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var filmStripAdapter: FilmStripAdapter
    private val viewModel: CameraViewModel by viewModels()

    private val audioManager by lazy {
        requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }


    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var zoomHideRunnable: Runnable? = null
    private var vibrator: android.os.Vibrator? = null

    private val ZOOM_SENSITIVITY = 2.4f

    private var uploadSession = UploadSession.GENERAL


    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            UiMessageManager.emitMessage(UiMessageState.Error("Kamera kullanabilmek için kamera izni gereklidir."))
            SmartNavigationEngine.navigateBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar_Fullscreen)

        val sessionKey = arguments?.getString(ARG_SESSION) ?: uploadSession.sessionKey
        uploadSession = UploadSession.fromKey(sessionKey)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSystemBarPadding()

        handleBackPressWithEngine()

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(true)

        cameraExecutor = Executors.newSingleThreadExecutor()

        vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator

        viewModel.initializeSession(uploadSession)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUi()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        setSystemBarsTheme(isCameraMode = true)

        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            window.setWindowAnimations(android.R.style.Animation_Activity)
        }
    }

    private fun setupSystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )

            view.setPadding(0, insets.top, 0, insets.bottom)

            windowInsets
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider?.unbindAll()
                val camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    lensSelector,
                    preview,
                    imageCapture
                )
                cameraControl = camera?.cameraControl
                cameraInfo = camera?.cameraInfo

                setupZoomMechanics()
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Kamera başlatılamadı", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupUi() {
        binding.btnGallery.visibility = if (uploadSession.allowGallery) View.VISIBLE else View.GONE

        filmStripAdapter = FilmStripAdapter(
            onImageClick = { uri ->
                val image = viewModel.uiState.value.capturedImages.find { it.uri == uri }
                if (image != null) {
                    viewModel.selectImageForPreview(image)
                }
            },
            onImageDelete = { uri ->
                val image = viewModel.uiState.value.capturedImages.find { it.uri == uri }
                if (image != null) {
                    viewModel.removeImageFromStrip(image)
                }
            }
        )

        binding.recyclerViewFilmStrip.apply {
            adapter = filmStripAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 250
                removeDuration = 250
            }
        }

        binding.btnMenuApprove.setOnClickListener {
            val currentState = viewModel.uiState.value
            val previewedImage = currentState.previewedImage

            if (previewedImage != null) {
                if (previewedImage.source == ImageSource.TEMP_CACHE) {
                    viewModel.saveTempImageToGallery(requireContext(), previewedImage, shouldKeepInStrip = true)
                } else {
                    viewModel.exitPreviewMode()
                }
            }
        }

        binding.btnMenuRemoveFromStrip.setOnClickListener {
            showPreviewActionDialog()
        }


        binding.btnCaptureLayout.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.isCapturing || currentState.isProcessing) return@setOnClickListener

            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                viewModel.setCapturing(true)

                binding.btnCaptureLayout.animate().scaleX(0.86f).scaleY(0.86f).setDuration(70).withEndAction {
                    binding.btnCaptureLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                    capturePhoto()
                }.start()
            } else {
                UiMessageManager.emitMessage(UiMessageState.Info("Fotoğraf çekebilmek için kamera izni gereklidir."))
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // Kamera Çevir
        binding.btnFlipCamera.setOnClickListener { flipBtn ->
            flipBtn.animate().rotationBy(180f).setDuration(300).start()
            lensSelector = if (lensSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            startCamera()
        }

        binding.btnGallery.setOnClickListener {
            openGalleryBottomSheet()
        }
        binding.btnClose.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }
        binding.btnConfirmAll.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.capturedImages.isNotEmpty()) {
                when (uploadSession) {
                    UploadSession.REPORT -> SmartNavigationEngine.navigateBack()
                    UploadSession.GENERAL -> SmartNavigationEngine.navigateTo(Screen.UPLOAD)
                    else -> SmartNavigationEngine.navigateTo(Screen.MAP)
                }
            } else {
                UiMessageManager.emitMessage(
                    UiMessageState.Error("Lütfen önce en az bir fotoğraf çekin!")
                )
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { state ->
                renderUiState(state)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is CameraUiEvent.ShowToast -> {
                        if (event.isSuccess) UiMessageManager.emitMessage(UiMessageState.Success(event.message))
                        else UiMessageManager.emitMessage(UiMessageState.Error(event.message))
                    }
                }
            }
        }
    }

    private fun renderUiState(state: CameraUiState) {
        filmStripAdapter.updateList(state.capturedImages.map { it.uri })

        binding.btnCaptureLayout.isEnabled = !state.isCapturing && !state.isProcessing && (state.capturedImages.size < uploadSession.maxImageCount)

        val hasImages =  state.capturedImages.isNotEmpty()

        binding.btnConfirmAll.apply {
            isClickable = hasImages
            isFocusable = hasImages
            imageTintList = ColorStateList.valueOf(
                if (hasImages) ContextCompat.getColor(requireContext(), R.color.catmap_accent)
                else ContextCompat.getColor(requireContext(), R.color.catmap_text_muted)
            )
        }

        if (hasImages) {
            binding.recyclerViewFilmStrip.smoothScrollToPosition(state.capturedImages.size - 1)
        }

        when (state.currentMode) {
            CameraMode.LIVE_PREVIEW -> {
                binding.ivInFragmentPreview.visibility = View.GONE
                binding.layoutPreviewMenu.visibility = View.GONE

                val accentColor = ContextCompat.getColor(requireContext(), R.color.catmap_accent)
                binding.btnCapture.background.mutate().setColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            CameraMode.IMAGE_PREVIEW -> {

                zoomHideRunnable?.let { binding.tvZoomRatio.removeCallbacks(it) }
                binding.tvZoomRatio.visibility = View.GONE
                binding.tvZoomRatio.alpha = 0f

                if (state.previewedImage != null) {
                    binding.ivInFragmentPreview.visibility = View.VISIBLE
                    Glide.with(this).load(state.previewedImage.uri).into(binding.ivInFragmentPreview)
                    binding.ivInFragmentPreview.alpha = 1f

                    binding.layoutPreviewMenu.visibility = View.VISIBLE
                    binding.layoutPreviewMenu.alpha = 1f

                    val successColor = ContextCompat.getColor(requireContext(), R.color.catmap_success)
                    binding.btnCapture.background.mutate().setColorFilter(successColor, android.graphics.PorterDuff.Mode.SRC_IN)
                }
            }
        }
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK)

        binding.viewFinder.alpha = 0.7f
        binding.viewFinder.animate().alpha(1.0f).setDuration(150).start()

        val cacheFile =
            File(requireContext().cacheDir, "CatMap_Temp_${System.currentTimeMillis()}.jpg")

        val metaData = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(cacheFile)
            .setMetadata(metaData)
            .build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(cacheFile)
                    viewModel.onPhotoCaptured(savedUri)
                }
                override fun onError(exception: ImageCaptureException) {
                    viewModel.setCapturing(false)
                    UiMessageManager.emitMessage(UiMessageState.Error("Fotoğraf çekilemedi."))
                }
            }
        )
    }


    private fun showPreviewActionDialog() {
        val dialogBinding = com.beem.catmap.databinding.DialogPreviewActionSheetBinding.inflate(layoutInflater)
        val actionDialog = android.app.AlertDialog.Builder(requireContext(), com.beem.catmap.R.style.CatMapDialogTheme)
            .setView(dialogBinding.root)
            .create()

        val currentState = viewModel.uiState.value
        val previewedImage = currentState.previewedImage ?: return

        when(previewedImage.source){
            ImageSource.TEMP_CACHE -> {
                dialogBinding.btnDialogSave.text = "Sadece Galeriye Kaydet"
                dialogBinding.btnDialogSave.visibility = View.VISIBLE
            }
            ImageSource.GALERI -> {
                dialogBinding.btnDialogSave.visibility = View.GONE
            }
        }

        dialogBinding.btnDialogDelete.setOnClickListener {
            viewModel.deleteImage(requireContext().contentResolver, previewedImage)
            actionDialog.dismiss()
        }

        dialogBinding.btnDialogSave.setOnClickListener {
            viewModel.saveTempImageToGallery(requireContext(), previewedImage, shouldKeepInStrip = false)
            actionDialog.dismiss()
        }

        dialogBinding.btnDialogRemoveFromStrip.setOnClickListener {
            viewModel.removeImageFromStrip(previewedImage)
            actionDialog.dismiss()
        }

        dialogBinding.btnDialogContinue.setOnClickListener {
            actionDialog.dismiss()
        }

        actionDialog.show()

        actionDialog.window?.apply {
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())

            // 📏 280dp'yi piksel cinsine dönüştürüyoruz (İstersen 260dp de yapabilirsin)
            val density = requireContext().resources.displayMetrics.density
            val widthInPx = (270 * density).toInt()

            // Window genişliğini tam olarak bu değere çiviliyoruz!
            setLayout(widthInPx, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(android.view.Gravity.CENTER)
        }
    }


    @android.annotation.SuppressLint("ClickableViewAccessibility", "DefaultLocale")
    private fun setupZoomMechanics() {
        val info = cameraInfo ?: return
        val control = cameraControl ?: return

        info.zoomState.observe(viewLifecycleOwner) { zoomState ->
            val ratioText = String.format("%.1fx", zoomState.zoomRatio)
            binding.tvZoomRatio.text = ratioText

            if (zoomState.zoomRatio % 1.0f < 0.05f && zoomState.zoomRatio > 1.05f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(8, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(8)
                }
            }
        }

        scaleGestureDetector = android.view.ScaleGestureDetector(requireContext(),
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    val currentZoomRatio = info.zoomState.value?.zoomRatio ?: 1f
                    val delta = 1.0f + (detector.scaleFactor - 1.0f) * ZOOM_SENSITIVITY
                    val targetZoomRatio = currentZoomRatio * delta

                    zoomHideRunnable?.let { binding.tvZoomRatio.removeCallbacks(it) }
                    binding.tvZoomRatio.alpha = 1.0f
                    binding.tvZoomRatio.visibility = View.VISIBLE

                    control.setZoomRatio(targetZoomRatio)
                    return true
                }
            }
        )

        binding.viewFinder.setOnTouchListener { v, event ->
            if (viewModel.uiState.value.currentMode == CameraMode.IMAGE_PREVIEW) {
                return@setOnTouchListener false
            }

            scaleGestureDetector.onTouchEvent(event)

            if (event.pointerCount > 1) {
                return@setOnTouchListener true
            }

            if (event.action == android.view.MotionEvent.ACTION_UP) {
                zoomHideRunnable?.let { binding.tvZoomRatio.removeCallbacks(it) }

                zoomHideRunnable = Runnable {
                    binding.tvZoomRatio.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction {
                            binding.tvZoomRatio.visibility = View.GONE
                        }
                        .start()
                }

                binding.tvZoomRatio.postDelayed(zoomHideRunnable, 1500)
            }

            v.onTouchEvent(event)
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _binding = null
    }


    private fun openGalleryBottomSheet() {
        if (isAdded && isResumed) {
            val gallerySheet = GalleryBottomSheet.newInstance(uploadSession)
            gallerySheet.show(childFragmentManager, "GalleryBottomSheet")
        }
    }

    override fun onResume() {
        super.onResume()
        setSystemBarsTheme(isCameraMode = true)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        setSystemBarsTheme(isCameraMode = false)
        cameraProvider?.unbindAll()
    }

    override fun onStop() {
        super.onStop()
        setSystemBarsTheme(isCameraMode = false)
    }

    private fun setSystemBarsTheme(isCameraMode: Boolean) {
        val window = requireActivity().window ?: return

        if (isCameraMode) {
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK

            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        } else {
            val originalBarColor = ContextCompat.getColor(requireContext(), com.beem.catmap.R.color.catmap_surface_white)
            window.statusBarColor = originalBarColor
            window.navigationBarColor = originalBarColor

            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true // true = Yazılar ve ikonlar KOYU olur
                isAppearanceLightNavigationBars = true
            }
        }
    }

    companion object {
        const val TAG = "CameraFragment"
        private const val ARG_SESSION = "arg_camera_session"

        fun newInstance(uploadSession: UploadSession = UploadSession.GENERAL) = CameraFragment().apply {
            arguments = newArgs(uploadSession)
        }

        fun newArgs(uploadSession: UploadSession = UploadSession.GENERAL): Bundle {
            return Bundle().apply {
                putString(ARG_SESSION, uploadSession.sessionKey)
            }
        }
    }

}