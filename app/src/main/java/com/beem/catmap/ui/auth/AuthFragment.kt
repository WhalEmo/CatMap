package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.beem.catmap.R
import com.beem.catmap.databinding.FragmentAuthBinding
import com.beem.catmap.ui.auth.exceptions.GoogleAuthException
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.getValue
import kotlin.time.Duration.Companion.milliseconds

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var dialog: BottomSheetDialog? = null
    private var isPasswordVisible = false
    private val db = FirebaseFirestore.getInstance()

    private val googleAuthClient by lazy { GoogleAuthClient(requireContext()) }

    private val TAG_GOOGLE_AUTH = "GoogleAuth"


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
            launchGoogleSignIn_v2()
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
        binding.btnGoogleGiris.isEnabled = false
        showLoadingPill("Google hesapları yükleniyor...")

        val request = googleAuthClient.buildGetCredentialRequest()

        // 🎯 Hileli 'post' yerine direkt coroutine scope'u güvenli şekilde çağırıyoruz
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = CredentialManager.create(requireContext()).getCredential(
                    request = request,
                    context = requireActivity() // Pencere hiyerarşisi için Activity context'i şart
                )

                // 🚀 Token'ı temizce ayıkla ve ViewModel'e uçur!
                googleAuthClient.extractIdToken(result.credential)?.let { idToken ->
                    viewModel.firebaseAuthWithGoogle(idToken)
                } ?: throw Exception("Token ayıklanamadı.")

            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Log.e("GOOGLE_AUTH", "Hata: ${e.javaClass.simpleName} | ${e.localizedMessage}")

                if (e.javaClass.simpleName.contains("Cancellation", ignoreCase = true)) {
                    showErrorPill("Giriş iptal edildi.")
                } else {
                    showErrorPill("Google hesabı şu an meşgul, tekrar deneyin.")
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_AUTH", "Genel Hata: ${e.localizedMessage}")
                showErrorPill("Giriş sırasında bir hata oluştu.")
            } finally {
                binding.btnGoogleGiris.isEnabled = true
            }
        }
    }

    private fun launchGoogleSignIn_v2() {
        if (!binding.btnGoogleGiris.isEnabled) {
            return
        }

        binding.btnGoogleGiris.isEnabled = false
        showLoadingPill("Google hesapları yükleniyor...")

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                val request = googleAuthClient.buildGetCredentialRequest()

                val result = googleAuthClient
                    .getCredentialManager()
                    .getCredential(
                        request = request,
                        context = requireActivity()
                    )

                val idToken = googleAuthClient.extractIdToken(
                    result.credential
                )

                Log.d(
                    TAG_GOOGLE_AUTH,
                    "Google credential başarıyla alındı."
                )

                viewModel.firebaseAuthWithGoogle(idToken)

            } catch (e: GetCredentialCancellationException) {

                Log.i(
                    TAG_GOOGLE_AUTH,
                    "Google Sign-In kullanıcı tarafından iptal edildi.",
                    e
                )

                showErrorPill("Google ile giriş iptal edildi.")

            } catch (e: NoCredentialException) {

                Log.w(
                    TAG_GOOGLE_AUTH,
                    "Cihazda kullanılabilir Google credential bulunamadı.",
                    e
                )

                showErrorPill(
                    "Kullanılabilir Google hesabı bulunamadı."
                )

            } catch (e: GetCredentialUnsupportedException) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "Credential Manager bu cihazda desteklenmiyor.",
                    e
                )

                showErrorPill(
                    "Bu cihaz Google ile girişi desteklemiyor."
                )

            } catch (e: GoogleAuthException.UnsupportedCredential) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "Beklenmeyen credential: ${e.credentialType}",
                    e
                )

                showErrorPill(
                    "Google hesabı doğrulanamadı."
                )

            } catch (e: GoogleAuthException.InvalidGoogleCredential) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "Google credential parse edilemedi.",
                    e
                )

                showErrorPill(
                    "Google hesabı doğrulanamadı. Lütfen tekrar deneyin."
                )

            } catch (e: GoogleAuthException.EmptyIdToken) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "Google ID token boş döndü.",
                    e
                )

                showErrorPill(
                    "Google doğrulaması tamamlanamadı."
                )

            } catch (e: CancellationException) {

                // Coroutine lifecycle nedeniyle iptal edildiyse
                // ASLA normal hata gibi tüketme.
                throw e

            } catch (e: GetCredentialException) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    """
                Credential Manager beklenmeyen hata:
                type=${e.javaClass.simpleName}
                message=${e.message}
                """.trimIndent(),
                    e
                )

                showErrorPill(
                    "Google ile giriş şu anda tamamlanamadı."
                )

            } catch (e: Exception) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "Google Sign-In beklenmeyen hata.",
                    e
                )

                showErrorPill(
                    "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin."
                )

            } finally {

                if (
                    isAdded &&
                    view != null
                ) {
                    binding.btnGoogleGiris.isEnabled = true
                }
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