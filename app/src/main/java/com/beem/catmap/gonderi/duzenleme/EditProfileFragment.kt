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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.gonderi.ProfileUpdateResult
import com.beem.catmap.gonderi.ProfileViewModel
import com.beem.catmap.gonderi.UiState
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

    // YENİ EKLENEN VİEW TANIŞTIRMALARI
    private lateinit var inputLayoutAd: TextInputLayout
    private lateinit var editAd: TextInputEditText
    private lateinit var inputLayoutSoyad: TextInputLayout
    private lateinit var editSoyad: TextInputEditText

    private lateinit var inputLayoutBio: TextInputLayout
    private lateinit var editBio: TextInputEditText

    private var selectedImageUri: Uri? = null
    private val currentUserId: String = UserSession.userId

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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
            profileViewModel.profilBilgileriniYukle(currentUserId)
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

        // AD VE SOYAD FİNDVIEWBYID
        inputLayoutAd = view.findViewById(R.id.inputLayoutAd)
        editAd = view.findViewById(R.id.editAd)
        inputLayoutSoyad = view.findViewById(R.id.inputLayoutSoyad)
        editSoyad = view.findViewById(R.id.editSoyad)

        inputLayoutBio = view.findViewById(R.id.inputLayoutBio)
        editBio = view.findViewById(R.id.editBio)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            klavyeyiKapat()
            SmartNavigationEngine.navigateBack()
        }

        val fotoSecAction = View.OnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                    profileViewModel.userProfile.collect { state ->
                        if (state is UiState.Success) {
                            val data = state.data
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
                                editBio.setText(data.hakkinda)
                            }
                            if (selectedImageUri == null && !data.fotoUrl.isNullOrBlank()) {
                                Glide.with(this@EditProfileFragment)
                                    .load(data.fotoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .into(profilFotoImageView)
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
                                Toast.makeText(requireContext(), "Profil başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                                profileViewModel.resetUpdateState()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                            is ProfileUpdateResult.UsernameAlreadyTaken -> {
                                setLoadingState(false)
                                inputLayoutKullaniciAdi.error = "Bu kullanıcı adı zaten kullanılmakta."
                                profileViewModel.resetUpdateState()
                            }
                            is ProfileUpdateResult.Error -> {
                                setLoadingState(false)
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
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

    private fun guncellemeyiBaslat() {
        val yeniKullaniciAdi = editKullaniciAdi.text?.toString()?.trim().orEmpty()
        val yeniAd = editAd.text?.toString()?.trim().orEmpty()
        val yeniSoyad = editSoyad.text?.toString()?.trim().orEmpty()
        val yeniBio = editBio.text?.toString()?.trim().orEmpty()

        inputLayoutKullaniciAdi.error = null

        if (yeniKullaniciAdi.isBlank()) {
            inputLayoutKullaniciAdi.error = "Kullanıcı adı boş bırakılamaz."
            return
        }

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
        kaydetButonu.isEnabled = !isLoading
        editKullaniciAdi.isEnabled = !isLoading
        editAd.isEnabled = !isLoading
        editSoyad.isEnabled = !isLoading
        editBio.isEnabled = !isLoading
        btnCameraBadge.isEnabled = !isLoading
        fotoDegistirText.isEnabled = !isLoading
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