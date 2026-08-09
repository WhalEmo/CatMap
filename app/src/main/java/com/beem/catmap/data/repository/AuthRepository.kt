package com.beem.catmap.data.repository

import com.beem.catmap.KullaniciAuth.DogrulamaKodYonetici
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.ui.auth.GoogleAuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class AuthRepository {

    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also {
                    INSTANCE = it
                }
            }
        }
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authYonetici: DogrulamaKodYonetici = DogrulamaKodYonetici()

    private val userNameTag = "KullaniciAdi"


    suspend fun login(username: String, password: String): Result<Kullanici> {
        return try {
            val query = db.collection("users")
                .whereEqualTo("KullaniciAdi", username)
                .limit(1)
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(Exception("Kullanıcı adı bulunamadı!"))
            }

            val doc = query.documents[0]
            val user = Kullanici(username, password).apply {
                kullaniciAdi = doc.getString("KullaniciAdi") ?: username
                ad = doc.getString("Ad") ?: ""
                soyad = doc.getString("Soyad") ?: ""
                email = doc.getString("Email") ?: ""
                fotoUrl = doc.getString("profilFotoUrl") ?: ""
                biyografi = doc.getString("Hakkinda") ?: ""
                takipEdilenSayisi = doc.getLong("TakipEdilenSayisi")
                takipciSayisi = doc.getLong("takipciSayisi")
                gonderiSayisi = doc.getLong("gonderiSayisi") ?: 0L
                id = doc.id
            }

            if (user.email.isEmpty()) {
                return Result.failure(Exception("Kullanıcı mail bilgisi eksik!"))
            }

            // Callback yapısını Coroutine'e çeviriyoruz
            val girisBasarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.girisYap(user.email, password) { basarili ->
                    if (continuation.isActive) continuation.resume(basarili)
                }
            }

            if (girisBasarili) {
                Result.success(user)
            } else {
                Result.failure(Exception("Şifre hatalı veya giriş başarısız!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 📝 Yeni Kullanıcı Kaydı
    suspend fun register(user: Kullanici): Result<Kullanici> {
        return try {
            val emailSonuc = db.collection("users").whereEqualTo("Email", user.email).get().await()
            if (!emailSonuc.isEmpty) {
                return Result.failure(Exception("Email ile daha önce kayıt yapılmış."))
            }

            val userSonuc = db.collection("users").whereEqualTo("KullaniciAdi", user.kullaniciAdi).get().await()
            if (!userSonuc.isEmpty) {
                return Result.failure(Exception("Bu kullanıcı adı zaten alınmış."))
            }

            val kayitBasarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.kaydetSifreEmail(user.email, user.sifre) { basarili ->
                    if (continuation.isActive) continuation.resume(basarili)
                }
            }

            if (!kayitBasarili) {
                return Result.failure(Exception("Email/Şifre kaydı başarısız!"))
            }

            val currentUid = mAuth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı ID alınamadı!"))

            db.collection("users").document(currentUid).set(user.KullaniciData()).await()
            user.id = currentUid

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔐 Şifre Sıfırlama
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val basarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.sifreSifirla(email) { sonuc ->
                    if (continuation.isActive) continuation.resume(sonuc)
                }
            }

            if (basarili) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("E-posta gönderilemedi!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🌐 Google ile Giriş & Kayıt
    suspend fun signInWithGoogle(idToken: String): Result<GoogleAuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = mAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Kullanıcı bilgileri alınamadı."))

            val uid = firebaseUser.uid
            val userRef = db.collection("users").document(uid)
            val doc = userRef.get().await()

            if (doc.exists() && !doc.getString(userNameTag).isNullOrBlank()) {
                val user = Kullanici(
                    kullaniciAdi = doc.getString("KullaniciAdi") ?: (firebaseUser.email?.substringBefore("@") ?: ""),
                    sifre = ""
                ).apply {
                    id = uid
                    ad = doc.getString("Ad") ?: (firebaseUser.displayName ?: "")
                    soyad = doc.getString("Soyad") ?: ""
                    email = doc.getString("Email") ?: (firebaseUser.email ?: "")
                    fotoUrl = doc.getString("profilFotoUrl") ?: (firebaseUser.photoUrl?.toString() ?: "")
                    biyografi = doc.getString("Hakkinda") ?: ""
                    takipEdilenSayisi = doc.getLong("TakipEdilenSayisi")
                    takipciSayisi = doc.getLong("takipciSayisi")
                    gonderiSayisi = doc.getLong("gonderiSayisi") ?: 0L
                }
                Result.success(GoogleAuthResult.ExistingUser(user))
            } else {
                val newUser = Kullanici(
                    ad = firebaseUser.displayName ?: "",
                    soyad = "",
                    email = firebaseUser.email ?: "",
                    kullaniciAdi = firebaseUser.email?.substringBefore("@") ?: "user_${uid.take(5)}",
                ).apply {
                    id = uid
                    fotoUrl = firebaseUser.photoUrl?.toString() ?: ""
                }
                Result.success(GoogleAuthResult.NewUser(newUser))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}