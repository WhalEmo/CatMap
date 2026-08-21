package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Gravity
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
                if (childFragmentManager.findFragmentByTag(AuthBottomSheetFragment.TAG) == null) {
                    AuthBottomSheetFragment.newInstance(AuthMode.REGISTER).show(
                        childFragmentManager,
                        AuthBottomSheetFragment.TAG
                    )
                }
            }
        }

        binding.btnUsernameGiris.setOnClickListener { view ->
            view.post {
                if (childFragmentManager.findFragmentByTag(AuthBottomSheetFragment.TAG) == null) {
                    AuthBottomSheetFragment.newInstance(AuthMode.LOGIN).show(
                        childFragmentManager,
                        AuthBottomSheetFragment.TAG
                    )
                }
            }
        }

        binding.btnGoogleGiris.setOnClickListener {
            launchGoogleSignIn_v3()
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
            .setMessageGravity(Gravity.START)
            .setPositiveButton("Anladım")
            .show(childFragmentManager, "CatMapLegalDialog")
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

                Log.d(
                    TAG_GOOGLE_AUTH,
                    e.toString(),
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


    private fun launchGoogleSignIn_v3() {
        if (!binding.btnGoogleGiris.isEnabled) {
            return
        }

        logGoogleAuthEnvironment()

        binding.btnGoogleGiris.isEnabled = false
        showLoadingPill("Google hesapları yükleniyor...")

        viewLifecycleOwner.lifecycleScope.launch {

            val startTime = android.os.SystemClock.elapsedRealtime()

            try {
                Log.i(
                    TAG_GOOGLE_AUTH,
                    "[CM-01] Credential request hazırlanıyor."
                )

                val request = googleAuthClient.buildGetCredentialRequest()

                Log.i(
                    TAG_GOOGLE_AUTH,
                    "[CM-02] getCredential() ÇAĞRISI BAŞLIYOR."
                )

                val result = googleAuthClient
                    .getCredentialManager()
                    .getCredential(
                        request = request,
                        context = requireActivity()
                    )

                val elapsed =
                    android.os.SystemClock.elapsedRealtime() - startTime

                Log.i(
                    TAG_GOOGLE_AUTH,
                    """
                [CM-03] getCredential() BAŞARILI.
                elapsedMs=$elapsed
                credentialClass=${result.credential.javaClass.name}
                credentialType=${result.credential.type}
                """.trimIndent()
                )

                Log.i(
                    TAG_GOOGLE_AUTH,
                    "[TOKEN-01] Credential parse başlıyor."
                )

                val idToken = googleAuthClient.extractIdToken(
                    result.credential
                )

                Log.i(
                    TAG_GOOGLE_AUTH,
                    "[TOKEN-02] ID token alındı. tokenPresent=${idToken.isNotBlank()}"
                )

                Log.i(
                    TAG_GOOGLE_AUTH,
                    "[FIREBASE-01] Firebase auth'a geçiliyor."
                )

                viewModel.firebaseAuthWithGoogle(idToken)

            } catch (e: GetCredentialCancellationException) {

                val elapsed =
                    android.os.SystemClock.elapsedRealtime() - startTime

                Log.e(
                    TAG_GOOGLE_AUTH,
                    """
                ===== GET CREDENTIAL CANCELLATION =====
                stage=getCredential
                elapsedMs=$elapsed
                exceptionClass=${e.javaClass.name}
                message=${e.message}
                localizedMessage=${e.localizedMessage}
                causeClass=${e.cause?.javaClass?.name}
                causeMessage=${e.cause?.message}
                suppressedCount=${e.suppressed.size}
                =======================================
                """.trimIndent(),
                    e
                )

                showErrorPill(
                    "Google hesabı doğrulanamadı. Lütfen tekrar deneyin."
                )

            } catch (e: NoCredentialException) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "[CM-ERROR] NoCredentialException: ${e.message}",
                    e
                )

                showErrorPill(
                    "Kullanılabilir Google hesabı bulunamadı."
                )

            } catch (e: GetCredentialUnsupportedException) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "[CM-ERROR] Credential Manager unsupported: ${e.message}",
                    e
                )

                showErrorPill(
                    "Bu cihaz Google ile girişi desteklemiyor."
                )

            } catch (e: GoogleAuthException.UnsupportedCredential) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "[TOKEN-ERROR] Unsupported credential=${e.credentialType}",
                    e
                )

                showErrorPill(
                    "Google hesabı doğrulanamadı."
                )

            } catch (e: GoogleAuthException.InvalidGoogleCredential) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "[TOKEN-ERROR] Credential parse başarısız.",
                    e
                )

                showErrorPill(
                    "Google hesabı doğrulanamadı. Lütfen tekrar deneyin."
                )

            } catch (e: GoogleAuthException.EmptyIdToken) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    "[TOKEN-ERROR] ID token boş.",
                    e
                )

                showErrorPill(
                    "Google doğrulaması tamamlanamadı."
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: GetCredentialException) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    """
                [CM-UNKNOWN-ERROR]
                class=${e.javaClass.name}
                message=${e.message}
                cause=${e.cause?.javaClass?.name}
                causeMessage=${e.cause?.message}
                """.trimIndent(),
                    e
                )

                showErrorPill(
                    "Google ile giriş şu anda tamamlanamadı."
                )

            } catch (e: Exception) {

                Log.e(
                    TAG_GOOGLE_AUTH,
                    """
                [AUTH-UNKNOWN-ERROR]
                class=${e.javaClass.name}
                message=${e.message}
                cause=${e.cause?.javaClass?.name}
                causeMessage=${e.cause?.message}
                """.trimIndent(),
                    e
                )

                showErrorPill(
                    "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin."
                )

            } finally {

                if (isAdded && view != null) {
                    binding.btnGoogleGiris.isEnabled = true
                }
            }
        }
    }


    private fun logGoogleAuthEnvironment() {
        try {
            val context = requireContext()
            val packageManager = context.packageManager

            val packageInfo =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.GET_SIGNATURES
                    )
                }

            val signatures =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners.orEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures.orEmpty()
                }

            val sha1List = signatures.map { signature ->
                val digest = java.security.MessageDigest
                    .getInstance("SHA-1")
                    .digest(signature.toByteArray())

                digest.joinToString(":") {
                    "%02X".format(it.toInt() and 0xFF)
                }
            }

            val playServicesInfo = runCatching {
                packageManager.getPackageInfo(
                    "com.google.android.gms",
                    0
                )
            }.getOrNull()

            Log.i(
                TAG_GOOGLE_AUTH,
                """
            ===== GOOGLE AUTH ENVIRONMENT =====
            package=${context.packageName}
            androidSdk=${android.os.Build.VERSION.SDK_INT}
            device=${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            googlePlayServices=${playServicesInfo?.versionName}
            installedSignerCount=${sha1List.size}
            installedSignerSha1=${sha1List.joinToString(" | ")}
            ===================================
            """.trimIndent()
            )

        } catch (e: Exception) {
            Log.e(
                TAG_GOOGLE_AUTH,
                "Google Auth environment okunamadı.",
                e
            )
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
                                    args = ProfileSetupFragment.newBundle(event.userModel),
                                    key = event.userModel.id
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