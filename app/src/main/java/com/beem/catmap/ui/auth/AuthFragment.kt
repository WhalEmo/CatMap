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
import kotlinx.coroutines.launch
import kotlin.getValue

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
            } catch (e: Exception) {
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
                                binding.btnGoogleGiris.isEnabled = true
                            }
                            is AuthUiState.Loading -> {
                                binding.btnGoogleGiris.isEnabled = false
                            }
                            is AuthUiState.Success -> {
                                binding.btnGoogleGiris.isEnabled = true
                                CurrentUserManager.getInstance(requireContext()).setCurrentUser(state.user)
                            }
                            is AuthUiState.Error -> {
                                binding.btnGoogleGiris.isEnabled = true
                                viewModel.resetState()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uyariMesaji = UyariMesaji(requireContext(), false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.girisid.setOnClickListener { openLogin() }
        binding.kaydolid.setOnClickListener { openRegister() }
    }


    override fun onResume() {
        super.onResume()
        resetButtonsState()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            resetButtonsState()
        }
    }

    private fun openLogin() {
        dialog?.dismiss()
        val loginBinding = GirispencereBinding.inflate(layoutInflater)

        isPasswordVisible = false

        loginBinding.eyeIcon.setOnClickListener {
            togglePasswordVisibility(loginBinding.passwordEditText, loginBinding.eyeIcon)
        }

        loginBinding.loginButton.setOnClickListener {
            handleLogin(loginBinding)
        }
        loginBinding.forgotPassword.setOnClickListener {
            openForgotPassword()
        }

        dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(loginBinding.root)
            show()
        }
    }

    private fun handleLogin(loginBinding: GirispencereBinding) {
        val username = loginBinding.usernameEditText.text.toString().trim()
        val password = loginBinding.passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            uyariMesaji.BasarisizDurum("Lütfen tüm alanları doldurun", 1000)
            return
        }

        uyariMesaji.YuklemeDurum("Giriş Yapılıyor...")
        val user = Kullanici(username, password)

        db.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!isAdded) return@addOnSuccessListener

                if (query.isEmpty) {
                    uyariMesaji.BasarisizDurum("Kullanıcı adı bulunamadı!", 1000)
                    return@addOnSuccessListener
                }

                val doc = query.documents[0]
                user.kullaniciAdi = doc.getString("KullaniciAdi") ?: username // <-- EKLENDİ
                user.ad = doc.getString("Ad")?:""
                user.soyad = doc.getString("Soyad")?:""
                user.email = doc.getString("Email")?:""
                user.fotoUrl = doc.getString("profilFotoUrl")?:""
                user.biyografi = doc.getString("Hakkinda")?:""
                user.takipEdilenSayisi = doc.getLong("TakipEdilenSayisi")
                user.takipciSayisi = doc.getLong("takipciSayisi")
                user.gonderiSayisi = doc.getLong("gonderiSayisi") ?: 0L

                user.id=(doc.id)

                if (user.email.isNullOrEmpty()) {
                    Log.e("AUTH_DEBUG", "CRITICAL HATA: Firestore'dan 'Email' alanı boş veya null geldi!")
                    uyariMesaji.BasarisizDurum("Kullanıcı mail bilgisi eksik!", 1000)
                    return@addOnSuccessListener
                }

                Log.d("AUTH_DEBUG", "Firebase Auth'a mail ve şifre gönderiliyor... (Email: ${user.email})")

                val ynt = DogrulamaKodYonetici()
                ynt.girisYap(user.email, user.sifre) { basarili ->
                    if (basarili) {
                        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                        Log.d("AUTH_DEBUG", ">>> GİRİŞ BAŞARILI! Firebase Auth Current User UID: $currentUid")

                        saveUserLocallyAndNavigate(user)
                        uyariMesaji.BasariliDurum("Giriş Başarılı...", 1000)
                    } else {
                        uyariMesaji.BasarisizDurum("Giriş Başarısız...", 1000)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AUTH_DEBUG", "CRITICAL HATA: Firestore sorgusu tamamen FAILED oldu!", exception)
                if (isAdded) {
                    uyariMesaji.BasarisizDurum("Bağlantı hatası oluştu!", 1000)
                }
                Log.d("AUTH_DEBUG", "--------------------------------------------------")
            }
    }

    private fun openRegister() {
        dialog?.dismiss()
        val registerBinding = KaydolpencereBinding.inflate(layoutInflater)

        isPasswordVisible = false

        registerBinding.eyeIcon.setOnClickListener {
            togglePasswordVisibility(registerBinding.passwordEditText, registerBinding.eyeIcon)
        }

        registerBinding.registerButton.setOnClickListener {
            handleRegister(registerBinding)
        }

        dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(registerBinding.root)
            show()
        }
    }


    private fun handleRegister(registerBinding: KaydolpencereBinding) {
        val user = Kullanici(
            ad = registerBinding.adEditText.text.toString().trim(),
            soyad = registerBinding.soyadEditText.text.toString().trim(),
            email = registerBinding.emailEditText.text.toString().trim(),
            kullaniciAdi = registerBinding.usernameEditText.text.toString().trim(),
            sifre = registerBinding.passwordEditText.text.toString().trim()
        )

        if (!Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            uyariMesaji.BasarisizDurum("Lütfen geçerli bir email adresi giriniz!", 1000)
            return
        }
        if (user.sifre.length < 5) {
            uyariMesaji.BasarisizDurum("Lütfen şifreyi en az 5 haneli giriniz!", 1000)
            return
        }

        uyariMesaji.YuklemeDurum("Kayıt Yapılıyor...")

        db.collection("users")
            .whereEqualTo("Email", user.email)
            .get()
            .addOnSuccessListener { sonuc ->
                if (!isAdded) return@addOnSuccessListener

                if (!sonuc.isEmpty) {
                    uyariMesaji.BasarisizDurum("Email ile daha önce kayıt yapılmış.", 1000)
                } else {
                    db.collection("users")
                        .whereEqualTo("KullaniciAdi", user.kullaniciAdi)
                        .get()
                        .addOnSuccessListener { cevap ->
                            if (!isAdded) return@addOnSuccessListener

                            if (!cevap.isEmpty) {
                                uyariMesaji.BasarisizDurum("Bu kullanıcı adı ile daha önce kayıt yapılmış.", 1000)
                            } else {
                                val ynt = DogrulamaKodYonetici()
                                ynt.kaydetSifreEmail(user.email, user.sifre) { basarili ->
                                    if (basarili) {
                                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                                        if (currentUserUid != null) {
                                            db.collection("users")
                                                .document(currentUserUid)
                                                .set(user.KullaniciData())
                                                .addOnSuccessListener {
                                                    if (!isAdded) return@addOnSuccessListener

                                                    user.id = currentUserUid // YENİ: setID yerine id alanına atama yapıldı
                                                    OnlinePresenceManager.setUserOnline()
                                                    saveUserLocallyAndNavigate(user)
                                                    uyariMesaji.BasariliDurum("Kayıt Başarılı...", 1000)
                                                }
                                                .addOnFailureListener {
                                                    if (isAdded) uyariMesaji.BasarisizDurum("Kayıt Başarısız!", 1000)
                                                }
                                        } else {
                                            if (isAdded) uyariMesaji.BasarisizDurum("Kullanıcı UID alınamadı!", 1000)
                                        }
                                    } else {
                                        if (isAdded) Toast.makeText(requireContext(), "Email veya şifre kaydı başarısız", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                }
            }
    }

    private fun openForgotPassword() {
        dialog?.dismiss()
        val forgotBinding = SifremiUnuttumBinding.inflate(layoutInflater)

        forgotBinding.resetPasswordButton.setOnClickListener { viewBtn ->
            viewBtn.isClickable = false
            uyariMesaji.YuklemeDurum("Mail Gönderiliyor...")
            val ynt = DogrulamaKodYonetici()
            val email = forgotBinding.emailEditText.text.toString().trim()

            ynt.sifreSifirla(email) { basarili ->
                if (basarili) {
                    uyariMesaji.BasariliDurum("Mail Gönderildi.", 1000)
                } else {
                    uyariMesaji.BasarisizDurum("Mail Gönderilemedi.", 1000)
                    viewBtn.isClickable = true
                }
            }
        }

        dialog = BottomSheetDialog(requireContext()).apply {
            setContentView(forgotBinding.root)
            show()
        }
    }

    private fun togglePasswordVisibility(passwordEdit: EditText, eyeIcon: ImageView) {
        val cursorPosition = passwordEdit.selectionStart
        if (!isPasswordVisible) {
            passwordEdit.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            eyeIcon.setImageResource(R.drawable.acik_goz)
        } else {
            passwordEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            eyeIcon.setImageResource(R.drawable.kapali_goz)
        }
        passwordEdit.setSelection(cursorPosition)
        isPasswordVisible = !isPasswordVisible
    }

    private fun saveUserLocallyAndNavigate(user: Kullanici) {
        CurrentUserManager.getInstance(requireContext()).setCurrentUser(user)
        dialog?.dismiss()
        animateButtonsOut()

        SmartNavigationEngine.navigateTo(Screen.MAP)
    }

    private fun animateButtonsOut() {
        binding.girisid.apply {
            isEnabled = true
            isClickable = true
            animate()
                .translationX(width + 2000f)
                .setDuration(1000)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }
        binding.kaydolid.apply {
            isEnabled = true
            isClickable = true
            animate()
                .translationX(width - 2000f)
                .setDuration(1000)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }
    }


    private fun resetButtonsState() {
        binding.girisid.apply {
            animate().cancel()
            translationX = 0f
            alpha = 1.0f
            isEnabled = true
            isClickable = true
        }
        binding.kaydolid.apply {
            animate().cancel()
            translationX = 0f
            alpha = 1.0f
            isEnabled = true
            isClickable = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dialog?.dismiss()
        dialog = null
        _binding = null
    }
    */

}