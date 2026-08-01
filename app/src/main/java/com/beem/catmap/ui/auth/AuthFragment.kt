package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.beem.catmap.CevrimIciYonetimi
import com.beem.catmap.KullaniciAuth.DogrulamaKodYonetici
import com.beem.catmap.KullaniciAuth.Kullanici

import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.ActivityMainBinding
import com.beem.catmap.databinding.GirispencereBinding
import com.beem.catmap.databinding.KaydolpencereBinding
import com.beem.catmap.databinding.SifremiUnuttumBinding
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class AuthFragment : Fragment() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var dialog: BottomSheetDialog? = null
    private var isPasswordVisible = false
    private lateinit var uyariMesaji: UyariMesaji
    private val db = FirebaseFirestore.getInstance()

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
            .get()
            .addOnSuccessListener { query ->
                if (!isAdded) return@addOnSuccessListener

                if (query.isEmpty) {
                    uyariMesaji.BasarisizDurum("Kullanıcı adı bulunamadı!", 1000)
                    return@addOnSuccessListener
                }

                val doc = query.documents[0]
                user.ad = doc.getString("Ad")
                user.soyad = doc.getString("Soyad")
                user.email = doc.getString("Email")
                user.setID(doc.id)

                val ynt = DogrulamaKodYonetici()
                ynt.girisYap(user.email, user.sifre) { basarili ->
                    if (basarili) {
                        CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi()
                        CevrimIciYonetimi.getInstance().CevrimIciCalistir(user)
                        saveUserLocallyAndNavigate(user)
                        uyariMesaji.BasariliDurum("Giriş Başarılı...", 1000)
                    } else {
                        uyariMesaji.BasarisizDurum("Giriş Başarısız...", 1000)
                    }
                }
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
        val user = Kullanici()
        user.ad = registerBinding.adEditText.text.toString().trim()
        user.soyad = registerBinding.soyadEditText.text.toString().trim()
        user.email = registerBinding.emailEditText.text.toString().trim()
        user.kullaniciAdi = registerBinding.usernameEditText.text.toString().trim()
        user.sifre = registerBinding.passwordEditText.text.toString().trim()

        if (!user.KullaniciIs()) {
            uyariMesaji.BasarisizDurum("Lütfen tüm alanları doldurun", 1000)
            return
        }
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
                                        db.collection("users")
                                            .add(user.KullaniciData())
                                            .addOnSuccessListener { docRef ->
                                                if (!isAdded) return@addOnSuccessListener

                                                user.setID(docRef.id)
                                                CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi()
                                                CevrimIciYonetimi.getInstance().CevrimIciCalistir(user)
                                                saveUserLocallyAndNavigate(user)
                                                uyariMesaji.BasariliDurum("Kayıt Başarılı...", 1000)
                                            }
                                            .addOnFailureListener {
                                                if (isAdded) uyariMesaji.BasarisizDurum("Kayıt Başarısız!", 1000)
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
}