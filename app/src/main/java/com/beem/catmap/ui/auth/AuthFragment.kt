package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.KullaniciAuth.DogrulamaKodYonetici
import com.beem.catmap.KullaniciAuth.Kullanici

import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.ActivityMainBinding
import com.beem.catmap.databinding.FragmentAuthBinding
import com.beem.catmap.databinding.GirispencereBinding
import com.beem.catmap.databinding.KaydolpencereBinding
import com.beem.catmap.databinding.SifremiUnuttumBinding
import com.beem.catmap.managers.OnlinePresenceManager
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.time.Duration.Companion.milliseconds

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var dialog: BottomSheetDialog? = null
    private var isPasswordVisible = false
    private val db = FirebaseFirestore.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTermsSpannableText()

        observeStateFlow()

        binding.btnAuthKaydol.setOnClickListener { view ->
            view.post {
                AuthBottomSheetFragment.newInstance(AuthMode.REGISTER).show(
                    childFragmentManager,
                    AuthBottomSheetFragment.TAG
                )
            }
        }
        binding.btnUsernameGiris.setOnClickListener { view ->
            view.post {
                AuthBottomSheetFragment.newInstance(AuthMode.LOGIN).show(
                    childFragmentManager,
                    AuthBottomSheetFragment.TAG
                )
            }
        }

        binding.btnGoogleGiris.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun setupTermsSpannableText() {
        val fullText = getString(R.string.user_agreement_and_privacy_policy)
        val spannableString = SpannableString(fullText)

        val termsTarget = "Kullanıcı Sözleşmesi"
        val privacyTarget = "Gizlilik Politikası"

        // 1. "Kullanici Sözlesmesi" Linki
        val termsClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showLegalDialog(
                    titleResId = R.string.user_agreement_title,
                    contentResId = R.string.user_agreement_content
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Altını çiz
                context?.let { ctx ->
                    ds.color = ContextCompat.getColor(ctx, R.color.catmap_primary) // Vurgulu renk ver
                }
            }
        }

        // 2. "Gizlilik Politikasi" Linki
        val privacyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                showLegalDialog(
                    titleResId = R.string.privacy_policy_title,
                    contentResId = R.string.privacy_policy_content
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Altını çiz
                context?.let { ctx ->
                    ds.color = ContextCompat.getColor(ctx, R.color.catmap_primary) // Vurgulu renk ver
                }
            }
        }

        // Metin içindeki kelime aralıkları (İndeksler)
        // "Kullanici Sözlesmesi" -> 15 ile 35 arası
        val termsStart = fullText.indexOf(termsTarget)
        if (termsStart != -1) {
            val termsEnd = termsStart + termsTarget.length
            spannableString.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // "Gizlilik Politikasi" -> 39 ile 58 arası
        val privacyStart = fullText.indexOf(privacyTarget)
        if (privacyStart != -1) {
            val privacyEnd = privacyStart + privacyTarget.length
            spannableString.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // TextView'a Spannable'ı çakıyoruz ve tıklama özelliğini aktif ediyoruz
        binding.txtAuthTerms.text = spannableString
        binding.txtAuthTerms.movementMethod = LinkMovementMethod.getInstance()
        context?.let { ctx ->
            binding.txtAuthTerms.highlightColor = ContextCompat.getColor(
                ctx,
                R.color.catmap_accent_alpha_15
            )
        }
    }

    private fun showLegalDialog(titleResId: Int, contentResId: Int) {
        CatMapDialog.build()
            .setTitle(getString(titleResId))
            .setMessage(getString(contentResId))
            .setPositiveButton("Anladım")
            .show(childFragmentManager, "CatMapLegalDialog")
    }


    private fun launchGoogleSignIn() {
        val credentialManager = CredentialManager.create(requireContext())

        // 1. Google ID Seçeneklerini Hazırla
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .setFilterByAuthorizedAccounts(false)
            .build()

        // 2. İstek Oluştur
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 3. Credential Manager'ı Çalıştır
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = requireContext()
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // 🚀 Firebase'e Token'ı Gönder
                    viewModel.firebaseAuthWithGoogle(idToken)
                }
            } catch (e: GetCredentialException) {
                showErrorPill("Google ile giriş işlemi kapatıldı!")
            } catch (e: Exception) {
                showErrorPill("Giriş işlemi başarısız oldu!")
            }
        }
    }

    private fun observeStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. UI State (Loading, Success, Error)
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is AuthUiState.Idle -> {
                                setButtonsEnabled(true)
                                hideStatusPill()
                            }
                            is AuthUiState.Loading -> {
                                setButtonsEnabled(false)
                                showLoadingPill(state.message)
                            }
                            is AuthUiState.Success -> {
                                setButtonsEnabled(true)
                                showSuccessPill(state.message)
                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(1000.milliseconds)
                                    viewModel.resetState()
                                }
                            }
                            is AuthUiState.Error -> {
                                setButtonsEnabled(true)
                                showErrorPill(state.errorMessage)
                            }
                        }
                    }
                }

                // 2. Event Dinleyicisi (Navigasyon)
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is AuthEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is AuthEvent.NavigateToMap -> {
                                SmartNavigationEngine.navigateTo(Screen.MAP)
                            }
                            is AuthEvent.NavigateToProfileSetup -> {
                                SmartNavigationEngine.navigateTo(
                                    targetScreen = Screen.PROFILE_SETUP,
                                    args = ProfileSetupFragment.newBundle(event.user),
                                    key = event.user.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔄 Yükleniyor Durumu (Primary Renk & Dönüş İndikatörü)
     */
    private fun showLoadingPill(message: String) {
        binding.tvStatusMessage.text = message
        binding.statusProgress.isVisible = true
        binding.imgStatusIcon.isVisible = false

        binding.tvStatusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.catmap_text_primary)
        )

        animatePillShow()
    }

    /**
     * 🟢 Başarılı Durumu (Yeşil Renk & Tik İkonu)
     */
    private fun showSuccessPill(message: String) {
        binding.tvStatusMessage.text = message
        binding.statusProgress.isVisible = false
        binding.imgStatusIcon.isVisible = true

        binding.imgStatusIcon.setImageResource(R.drawable.ic_check_circle)
        binding.imgStatusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        )
        binding.tvStatusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        )

        animatePillShow()


        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000.milliseconds)
            hideStatusPill()
        }
    }

    /**
     * 🔴 Hata Durumu (Kırmızı Renk, Hata İkonu & Otomatik Kaybolma)
     */
    private fun showErrorPill(message: String) {
        binding.tvStatusMessage.text = message
        binding.statusProgress.isVisible = false
        binding.imgStatusIcon.isVisible = true

        binding.imgStatusIcon.setImageResource(R.drawable.ic_error_outline)
        binding.imgStatusIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )
        binding.tvStatusMessage.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        )

        animatePillShow()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000.milliseconds)
            hideStatusPill()
            viewModel.resetState()
        }
    }

    private fun animatePillShow() {
        binding.statusPill.apply {
            if (!isVisible) {
                alpha = 0f
                isVisible = true
                animate().alpha(1f).setDuration(200).start()
            }
        }
    }

    private fun hideStatusPill() {
        _binding?.statusPill?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                _binding?.statusPill?.isVisible = false
            }
            ?.start()
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnGoogleGiris.isEnabled = enabled
        binding.btnUsernameGiris.isEnabled = enabled
        binding.btnAuthKaydol.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}