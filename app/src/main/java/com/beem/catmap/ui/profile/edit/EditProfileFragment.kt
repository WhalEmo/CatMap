package com.beem.catmap.ui.profile.edit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.ui.profile.common.ProfileViewModel
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.ProfileUpdateResult
import com.beem.catmap.ui.extensions.applyInputLimits
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.profile.common.UiState
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var saveButton: Button
    private lateinit var profilePhotoImageView: CircleImageView
    private lateinit var btnCameraBadge: MaterialCardView
    private lateinit var photoChangeText: TextView
    private lateinit var inputLayoutUsername: TextInputLayout
    private lateinit var editUsername: TextInputEditText

    private lateinit var inputLayoutName: TextInputLayout
    private lateinit var editName: TextInputEditText
    private lateinit var inputLayoutSurname: TextInputLayout
    private lateinit var editSurname: TextInputEditText

    private lateinit var inputLayoutBio: TextInputLayout
    private lateinit var editBio: TextInputEditText

    private lateinit var loadingOverlay: View

    private var selectedImageUri: Uri? = null
    private val currentUserId: String = UserSession.userId

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.kullanici)
                .into(profilePhotoImageView)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profili_duzenle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        observeViewModel()

        if (currentUserId.isNotBlank()) {
            profileViewModel.allProfileLoad(currentUserId, forceRefresh = false)
        }
        editUsername.applyInputLimits(maxLength = 20, maxLines = 2)
        editName.applyInputLimits(maxLength = 20, maxLines = 2)
        editSurname.applyInputLimits(maxLength = 20, maxLines = 2)
        editBio.applyInputLimits(maxLength = 150, maxLines = 10)
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        saveButton = view.findViewById(R.id.kaydetButonu)
        profilePhotoImageView = view.findViewById(R.id.profilFotoImageViewDuzenle)
        btnCameraBadge = view.findViewById(R.id.btnCameraBadge)
        photoChangeText = view.findViewById(R.id.fotoDegistirText)

        inputLayoutUsername = view.findViewById(R.id.inputLayoutKullaniciAdi)
        editUsername = view.findViewById(R.id.editKullaniciAdi)

        inputLayoutName = view.findViewById(R.id.inputLayoutAd)
        editName = view.findViewById(R.id.editAd)
        inputLayoutSurname = view.findViewById(R.id.inputLayoutSoyad)
        editSurname = view.findViewById(R.id.editSoyad)

        inputLayoutBio = view.findViewById(R.id.inputLayoutBio)
        editBio = view.findViewById(R.id.editBio)

        loadingOverlay = view.findViewById(R.id.loadingOverlay)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            closeKeyboard()
            clearEditState()
            SmartNavigationEngine.navigateBack()
        }

        val fotoSecAction = View.OnClickListener {
            pickMedia.launch("image/*")
        }

        btnCameraBadge.setOnClickListener(fotoSecAction)
        photoChangeText.setOnClickListener(fotoSecAction)

        saveButton.setOnClickListener {
            it.bounceAndHaptic()
            closeKeyboard()
            startUpdate()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    profileViewModel.fullProfileState.collect { state ->
                        if (state is UiState.Success) {
                            val data = state.data.profile

                            if (selectedImageUri == null && !data.photoUrl.isNullOrBlank()) {
                                Glide.with(this@EditProfileFragment)
                                    .load(data.photoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilePhotoImageView)
                            }

                            if (editUsername.text.isNullOrBlank()) {
                                editUsername.setText(data.username)
                            }
                            if (editName.text.isNullOrBlank()) {
                                editName.setText(data.name)
                            }
                            if (editSurname.text.isNullOrBlank()) {
                                editSurname.setText(data.surname)
                            }
                            if (editBio.text.isNullOrBlank()) {
                                editBio.setText(data.bio)
                            }
                        }
                    }
                }


                launch {
                    profileViewModel.profileUpdateState.collect { result ->
                        when (result) {
                            is ProfileUpdateResult.Loading -> {
                                setLoadingState(true)
                            }
                            is ProfileUpdateResult.Success -> {
                                setLoadingState(false)
                                UiMessageManager.emitMessage(UiMessageState.Success("Profil başarıyla güncellendi."))

                                val currentFullData = (profileViewModel.fullProfileState.value as? UiState.Success)?.data
                                val currentProfile = currentFullData?.profile
                                val guncelUserModel = currentProfile?.copy(
                                    username = result.newUsername,
                                    name = result.newName,
                                    surname = result.newSurname,
                                    bio = result.newBio,
                                    photoUrl = result.newPhotoUrl.takeIf { !it.isNullOrBlank() } ?: currentProfile.photoUrl
                                ) ?: UserModel().apply {
                                    id = currentUserId
                                    username = result.newUsername
                                    name = result.newName
                                    surname = result.newSurname
                                    bio = result.newBio
                                    photoUrl = result.newPhotoUrl ?: ""
                                }

                                // EventBus veya FragmentResult ile ilet
                                //ProfileEventBus.emitEvent(ProfileEvent.ProfileUpdated(guncelUserModel))

                                profileViewModel.resetUpdateState()
                                SmartNavigationEngine.navigateBack()
                            }
                            is ProfileUpdateResult.UsernameAlreadyTaken -> {
                                setLoadingState(false)
                                inputLayoutUsername.error = "Bu kullanıcı adı zaten kullanılmakta."
                                profileViewModel.resetUpdateState()
                            }
                            is ProfileUpdateResult.Error -> {
                                setLoadingState(false)
                                UiMessageManager.emitMessage(UiMessageState.Error(result.message))
                                profileViewModel.resetUpdateState()
                            }
                            ProfileUpdateResult.Idle -> {
                                setLoadingState(false)
                            }
                        }
                    }
                }
            }
        }
    }
    private fun clearEditState() {
        selectedImageUri = null
        val currentState = profileViewModel.fullProfileState.value
        if (currentState is UiState.Success && !currentState.data.profile.photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(currentState.data.profile.photoUrl)
                .placeholder(R.drawable.kullanici)
                .into(profilePhotoImageView)
        } else {
            profilePhotoImageView.setImageResource(R.drawable.kullanici)
        }

        profileViewModel.resetUpdateState()
    }

    private fun startUpdate() {
        val yeniKullaniciAdi = editUsername.text?.toString()?.trim().orEmpty()
        val yeniAd = editName.text?.toString()?.trim().orEmpty()
        val yeniSoyad = editSurname.text?.toString()?.trim().orEmpty()
        val yeniBio = editBio.text?.toString()?.trim().orEmpty()

        inputLayoutUsername.error = null
        inputLayoutName.error = null
        inputLayoutSurname.error = null

        var hasError = false

        if (yeniKullaniciAdi.isBlank()) {
            inputLayoutUsername.error = "Kullanıcı adı boş bırakılamaz."
            hasError = true
        }

        if (yeniAd.isBlank()) {
            inputLayoutName.error = "Ad boş bırakılamaz."
            hasError = true
        }

        if (yeniSoyad.isBlank()) {
            inputLayoutSurname.error = "Soyad boş bırakılamaz."
            hasError = true
        }

        if (hasError) return

        profileViewModel.allProfileUpdate(
            yeniKullaniciAdi = yeniKullaniciAdi,
            yeniAd = yeniAd,
            yeniSoyad = yeniSoyad,
            yeniHakkinda = yeniBio,
            yeniResimUri = selectedImageUri,
            currentUserId = currentUserId
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun closeKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileViewModel.resetUpdateState()
    }
}