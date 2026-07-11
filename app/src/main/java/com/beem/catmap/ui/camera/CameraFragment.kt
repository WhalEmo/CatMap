package com.beem.catmap.ui.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentCameraBinding
import com.beem.catmap.ui.manager.ImageUploadManager
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.bumptech.glide.Glide
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.fragment.app.DialogFragment
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine

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

    private var zoomHideRunnable: Runnable? = null
    private var vibrator: android.os.Vibrator? = null

    private val ZOOM_SENSITIVITY = 2.4f

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) startCamera()
        else {
            UiMessageManager.emitMessage(UiMessageState.Error("Kamera izinleri eksik!"))
            parentFragmentManager.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(true)

        cameraExecutor = Executors.newSingleThreadExecutor()

        vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator

        checkPermissionsAndStart()
        setupUi()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            window.setWindowAnimations(android.R.style.Animation_Activity)
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.READ_MEDIA_IMAGES)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startCamera() else requestPermissionsLauncher.launch(missing.toTypedArray())
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    lensSelector,
                    preview,
                    imageCapture
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo

                setupZoomMechanics()
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Kamera başlatılamadı", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupUi() {
        filmStripAdapter = FilmStripAdapter(
            onImageClick = { uri ->
                val imageSource = if (uri.path?.contains(requireContext().cacheDir.path) == true) {
                    ImageSource.TEMP_CACHE
                } else {
                    ImageSource.GALERI
                }
                viewModel.selectImageForPreview(uri, imageSource)
            },
            onImageDelete = { uri -> viewModel.removeImageFromStrip(uri) }
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
            val activeUri = currentState.activePreviewUri

            if (activeUri != null) {
                if (currentState.activeImageSource == ImageSource.TEMP_CACHE) {
                    viewModel.saveTempImageToGallery(requireContext(), activeUri, shouldKeepInStrip = true)
                } else {
                    viewModel.exitPreviewMode()
                }
            }
        }

        binding.btnMenuRemoveFromStrip.setOnClickListener {
            showPreviewActionDialog()
        }


        // Deklanşör
        binding.btnCaptureLayout.setOnClickListener {
            binding.btnCaptureLayout.animate().scaleX(0.86f).scaleY(0.86f).setDuration(70).withEndAction {
                binding.btnCaptureLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                capturePhoto()
            }.start()
        }

        // Kamera Çevir
        binding.btnFlipCamera.setOnClickListener { flipBtn ->
            flipBtn.animate().rotationBy(180f).setDuration(300).start()
            lensSelector = if (lensSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            startCamera()
        }

        binding.btnGallery.setOnClickListener {

        }
        binding.btnClose.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }
        binding.btnConfirmAll.setOnClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.capturedImages.isNotEmpty()) {
                SmartNavigationEngine.navigateTo(Screen.UPLOAD)
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
        filmStripAdapter.updateList(state.capturedImages)

        binding.btnCaptureLayout.isEnabled = state.capturedImages.size < 5

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
                if (state.activePreviewUri != null) {
                    binding.ivInFragmentPreview.visibility = View.VISIBLE
                    Glide.with(this).load(state.activePreviewUri).into(binding.ivInFragmentPreview)
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
        val activeUri = currentState.activePreviewUri ?: return

        when(currentState.activeImageSource){
            ImageSource.TEMP_CACHE -> {
                dialogBinding.btnDialogSave.text = "Sadece Galeriye Kaydet"
                dialogBinding.btnDialogSave.visibility = View.VISIBLE
            }
            ImageSource.GALERI -> {
                dialogBinding.btnDialogSave.visibility = View.GONE
            }
        }

        dialogBinding.btnDialogDelete.setOnClickListener {
            viewModel.deleteImage(requireContext().contentResolver, activeUri)
            actionDialog.dismiss()
        }

        // SADECE GALERİYE KAYDET: Sadece cache durumunda görünür ve tetiklenir
        dialogBinding.btnDialogSave.setOnClickListener {
            viewModel.saveTempImageToGallery(requireContext(), activeUri, shouldKeepInStrip = false)
            actionDialog.dismiss()
        }

        // ÖNİZLEMEYE DEVAM ET
        dialogBinding.btnDialogContinue.setOnClickListener {
            actionDialog.dismiss()
        }

        actionDialog.show()
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
        cameraExecutor.shutdown()
        _binding = null
    }
}