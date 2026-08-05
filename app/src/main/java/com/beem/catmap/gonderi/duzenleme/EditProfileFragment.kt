package com.beem.catmap.Profil

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
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.gonderi.ProfileUpdateResult
import com.beem.catmap.gonderi.ProfileViewModel
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var btnBack: ImageButton
    private lateinit var kaydetButonu: Button
    private lateinit var profilFotoImageView: CircleImageView
    private lateinit var btnCameraBadge: MaterialCardView
    private lateinit var fotoDegistirText: TextView
    private lateinit var inputLayoutKullaniciAdi: TextInputLayout
    private lateinit var editKullaniciAdi: TextInputEditText

    private lateinit var inputLayoutAd: TextInputLayout
    private lateinit var editAd: TextInputEditText
    private lateinit var inputLayoutSoyad: TextInputLayout
    private lateinit var editSoyad: TextInputEditText

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
                .into(profilFotoImageView)
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
            profileViewModel.tumProfilVerileriniYukle(currentUserId, forceRefresh = false)
        }
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        kaydetButonu = view.findViewById(R.id.kaydetButonu)
        profilFotoImageView = view.findViewById(R.id.profilFotoImageViewDuzenle)
        btnCameraBadge = view.findViewById(R.id.btnCameraBadge)
        fotoDegistirText = view.findViewById(R.id.fotoDegistirText)

        inputLayoutKullaniciAdi = view.findViewById(R.id.inputLayoutKullaniciAdi)
        editKullaniciAdi = view.findViewById(R.id.editKullaniciAdi)

        inputLayoutAd = view.findViewById(R.id.inputLayoutAd)
        editAd = view.findViewById(R.id.editAd)
        inputLayoutSoyad = view.findViewById(R.id.inputLayoutSoyad)
        editSoyad = view.findViewById(R.id.editSoyad)

        inputLayoutBio = view.findViewById(R.id.inputLayoutBio)
        editBio = view.findViewById(R.id.editBio)

        loadingOverlay = view.findViewById(R.id.loadingOverlay)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            klavyeyiKapat()
            clearEditState()
            SmartNavigationEngine.navigateBack()
        }

        val fotoSecAction = View.OnClickListener {
            pickMedia.launch("image/*")
        }

        btnCameraBadge.setOnClickListener(fotoSecAction)
        fotoDegistirText.setOnClickListener(fotoSecAction)

        kaydetButonu.setOnClickListener {
            klavyeyiKapat()
            guncellemeyiBaslat()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    profileViewModel.fullProfileState.collect { state ->
                        if (state is UiState.Success) {
                            val data = state.data.profile

                            if (selectedImageUri == null && !data.fotoUrl.isNullOrBlank()) {
                                Glide.with(this@EditProfileFragment)
                                    .load(data.fotoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilFotoImageView)
                            }

                            if (editKullaniciAdi.text.isNullOrBlank()) {
                                editKullaniciAdi.setText(data.kullaniciAdi)
                            }
                            if (editAd.text.isNullOrBlank()) {
                                editAd.setText(data.ad)
                            }
                            if (editSoyad.text.isNullOrBlank()) {
                                editSoyad.setText(data.soyad)
                            }
                            if (editBio.text.isNullOrBlank()) {
                                editBio.setText(data.biyografi)
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
                                profileViewModel.resetUpdateState()

                                // GEREKSİZ LOCAL NESNE OLUŞTURMA KALDIRILDI:
                                // Sunucudan/Storage'dan başarıyla dönen 'result' verisiyle doğrudan nesne oluşturulur.
                                val guncelKullanici = Kullanici().apply {
                                    id = currentUserId
                                    kullaniciAdi = result.newUsername
                                    ad = result.newAd
                                    soyad = result.newSoyad
                                    biyografi = result.newHakkinda
                                    fotoUrl = result.newPhotoUrl
                                }

                                lifecycleScope.launch {
                                    ProfileEventBus.emitEvent(ProfileEvent.ProfileUpdated(guncelKullanici))
                                }
                                SmartNavigationEngine.navigateBack()
                            }
                            is ProfileUpdateResult.UsernameAlreadyTaken -> {
                                setLoadingState(false)
                                inputLayoutKullaniciAdi.error = "Bu kullanıcı adı zaten kullanılmakta."
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
        if (currentState is UiState.Success && !currentState.data.profile.fotoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(currentState.data.profile.fotoUrl)
                .placeholder(R.drawable.kullanici)
                .into(profilFotoImageView)
        } else {
            profilFotoImageView.setImageResource(R.drawable.kullanici)
        }

        profileViewModel.resetUpdateState()
    }

    private fun guncellemeyiBaslat() {
        val yeniKullaniciAdi = editKullaniciAdi.text?.toString()?.trim().orEmpty()
        val yeniAd = editAd.text?.toString()?.trim().orEmpty()
        val yeniSoyad = editSoyad.text?.toString()?.trim().orEmpty()
        val yeniBio = editBio.text?.toString()?.trim().orEmpty()

        inputLayoutKullaniciAdi.error = null
        inputLayoutAd.error = null
        inputLayoutSoyad.error = null

        var hasError = false

        if (yeniKullaniciAdi.isBlank()) {
            inputLayoutKullaniciAdi.error = "Kullanıcı adı boş bırakılamaz."
            hasError = true
        }

        if (yeniAd.isBlank()) {
            inputLayoutAd.error = "Ad boş bırakılamaz."
            hasError = true
        }

        if (yeniSoyad.isBlank()) {
            inputLayoutSoyad.error = "Soyad boş bırakılamaz."
            hasError = true
        }

        if (hasError) return

        setLoadingState(true)
        profileViewModel.tumProfilBilgileriniGuncelle(
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

    private fun klavyeyiKapat() {
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