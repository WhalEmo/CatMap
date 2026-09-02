package com.beem.catmap.ui.profile_v2.edit

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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.R
import com.beem.catmap.ui.extensions.applyInputLimits
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private val viewModel: EditProfileViewModel by viewModels()

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

    private var isInitialDataSet = false

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
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
        setupLimits()
        setupListeners()
        observeUiState()
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

    private fun setupLimits() {
        editUsername.applyInputLimits(maxLength = 20, maxLines = 2)
        editName.applyInputLimits(maxLength = 20, maxLines = 2)
        editSurname.applyInputLimits(maxLength = 20, maxLines = 2)
        editBio.applyInputLimits(maxLength = 150, maxLines = 10)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            closeKeyboard()
            SmartNavigationEngine.navigateBack()
        }

        val selectPhotoAction = View.OnClickListener {
            pickMedia.launch("image/*")
        }
        btnCameraBadge.setOnClickListener(selectPhotoAction)
        photoChangeText.setOnClickListener(selectPhotoAction)

        saveButton.setOnClickListener {
            it.bounceAndHaptic()
            closeKeyboard()
            viewModel.saveProfile()
        }

        // Kullanıcı klavyeden yazdıkça State'i besle
        editUsername.doAfterTextChanged { viewModel.onUsernameChange(it?.toString().orEmpty()) }
        editName.doAfterTextChanged { viewModel.onNameChange(it?.toString().orEmpty()) }
        editSurname.doAfterTextChanged { viewModel.onSurnameChange(it?.toString().orEmpty()) }
        editBio.doAfterTextChanged { viewModel.onBioChange(it?.toString().orEmpty()) }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                        saveButton.isEnabled = !state.isLoading

                        if (!isInitialDataSet && state.initialUser != null) {
                            isInitialDataSet = true

                            editUsername.setText(state.username)
                            editName.setText(state.name)
                            editSurname.setText(state.surname)
                            editBio.setText(state.bio)

                            if (state.currentPhotoUrl.isNotBlank()) {
                                Glide.with(this@EditProfileFragment)
                                    .load(state.currentPhotoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilePhotoImageView)
                            }
                        }

                        // 3. Validasyon Hataları
                        inputLayoutUsername.error = state.usernameError
                        inputLayoutName.error = state.nameError
                        inputLayoutSurname.error = state.surnameError

                        // 4. Genel Hata Mesajı
                        state.errorMessage?.let { msg ->
                            UiMessageManager.emitMessage(UiMessageState.Error(msg))
                        }
                    }
                }

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is EditProfileEvent.SaveSuccess -> {
                                UiMessageManager.emitMessage(UiMessageState.Success("Profil başarıyla güncellendi."))
                                SmartNavigationEngine.navigateBack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun closeKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}