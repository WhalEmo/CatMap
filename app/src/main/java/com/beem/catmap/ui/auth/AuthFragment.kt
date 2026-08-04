package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
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

        Log.d("AUTH_DEBUG", "--------------------------------------------------")
        Log.d("AUTH_DEBUG", ">>> Giriş denemesi başladı. Kullanıcı Adı: '$username'")

        if (username.isEmpty() || password.isEmpty()) {
            Log.w("AUTH_DEBUG", "HATA: Kullanıcı adı veya şifre boş bırakıldı.")
            uyariMesaji.BasarisizDurum("Lütfen tüm alanları doldurun", 1000)
            return
        }

        uyariMesaji.YuklemeDurum("Giriş Yapılıyor...")
        val user = Kullanici(username, password)

        Log.d("AUTH_DEBUG", "Firestore 'users' koleksiyonunda 'KullaniciAdi == $username' sorgusu atılıyor...")

        db.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                Log.d("AUTH_DEBUG", "Firestore yanıt verdi. Bulunan doküman sayısı: ${query.size()}")

                if (!isAdded) {
                    Log.w("AUTH_DEBUG", "UYARI: Fragment (isAdded = false) durumunda, işlem iptal edildi.")
                    return@addOnSuccessListener
                }

                if (query.isEmpty) {
                    Log.e("AUTH_DEBUG", "HATA: Firestore'da '$username' kullanıcı adına sahip doküman BULUNAMADI.")
                    uyariMesaji.BasarisizDurum("Kullanıcı adı bulunamadı!", 1000)
                    return@addOnSuccessListener
                }

                val doc = query.documents[0]
                val emailFromDb = doc.getString("Email")

                Log.d("AUTH_DEBUG", "Kullanıcı Firestore'da bulundu! Doc ID (UID): ${doc.id} | Email: $emailFromDb")

                user.ad = doc.getString("Ad")
                user.soyad = doc.getString("Soyad")
                user.email = emailFromDb
                user.setID(doc.id)

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

                        CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi()
                        CevrimIciYonetimi.getInstance().CevrimIciCalistir(user)
                        saveUserLocallyAndNavigate(user)
                        uyariMesaji.BasariliDurum("Giriş Başarılı...", 1000)
                    } else {
                        Log.e("AUTH_DEBUG", "HATA: Firebase Auth maile/şifreye onay vermedi! (Giriş Başarısız)")
                        uyariMesaji.BasarisizDurum("Şifre hatalı veya giriş başarısız...", 1000)
                    }
                    Log.d("AUTH_DEBUG", "--------------------------------------------------")
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
                                        // Firebase Auth'ta oluşturan kullanıcının UID'sini alıyoruz
                                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

                                        if (currentUserUid != null) {
                                            // Doküman ID'sini rastgele üretmek (.add) yerine Auth UID yaparak (.document(uid).set) kaydediyoruz
                                            db.collection("users")
                                                .document(currentUserUid)
                                                .set(user.KullaniciData())
                                                .addOnSuccessListener {
                                                    if (!isAdded) return@addOnSuccessListener

                                                    user.setID(currentUserUid)
                                                    CevrimIciYonetimi.getInstance().AnasayfaArayuzAktivitiyeGecildi()
                                                    CevrimIciYonetimi.getInstance().CevrimIciCalistir(user)
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
}