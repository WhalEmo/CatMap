package com.beem.catmap.data.repository

import com.beem.catmap.userAuth.VerifyAuth
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.ui.auth.GoogleAuthResult
import com.beem.catmap.ui.auth.exceptions.AuthError
import com.beem.catmap.utils.CatLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
    private val authYonetici: VerifyAuth = VerifyAuth()

    private val userNameTag = "KullaniciAdi"


    suspend fun login(username: String, password: String): Result<UserModel> {
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
            val userModel = UserModel(username, password).apply {
                this.username = doc.getString("KullaniciAdi") ?: username
                name = doc.getString("Ad") ?: ""
                surname = doc.getString("Soyad") ?: ""
                email = doc.getString("Email") ?: ""
                photoUrl = doc.getString("profilFotoUrl") ?: ""
                bio = doc.getString("Hakkinda") ?: ""
                followingCount = doc.getLong("TakipEdilenSayisi")
                followersCount = doc.getLong("takipciSayisi")
                postCount = doc.getLong("gonderiSayisi") ?: 0L
                id = doc.id
            }

            if (userModel.email.isEmpty()) {
                return Result.failure(Exception("Kullanıcı mail bilgisi eksik!"))
            }

            val girisBasarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.login(userModel.email, password) { basarili ->
                    if (continuation.isActive) continuation.resume(basarili)
                }
            }

            if (girisBasarili) {
                Result.success(userModel)
            } else {
                Result.failure(Exception("Şifre hatalı veya giriş başarısız!"))
            }
        } catch (e: Exception) {
            CatLogger.logError("AuthReposıtory", "login", e)
            Result.failure(e)
        }
    }

    suspend fun register(userModel: UserModel): Result<UserModel> {
        return try {
            val emailSonuc = db.collection("users").whereEqualTo("Email", userModel.email).get().await()
            if (!emailSonuc.isEmpty) {
                return Result.failure(Exception("Email ile daha önce kayıt yapılmış."))
            }

            val userSonuc = db.collection("users").whereEqualTo("KullaniciAdi", userModel.username).get().await()
            if (!userSonuc.isEmpty) {
                return Result.failure(Exception("Bu kullanıcı adı zaten alınmış."))
            }

            val kayitBasarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.savePasswordEmail(userModel.email, userModel.password) { basarili ->
                    if (continuation.isActive) continuation.resume(basarili)
                }
            }

            if (!kayitBasarili) {
                return Result.failure(Exception("Email/Şifre kaydı başarısız!"))
            }

            val currentUid = mAuth.currentUser?.uid ?: return Result.failure(Exception("Kullanıcı ID alınamadı!"))

            db.collection("users").document(currentUid).set(userModel.KullaniciData()).await()

            val publicData = mapOf(
                "KullaniciAdi" to userModel.username,
                "FotoUrl" to (userModel.photoUrl ?: "")
            )
            db.collection("publicUsers").document(currentUid).set(publicData).await()

            userModel.id = currentUid

            Result.success(userModel)
        } catch (e: Exception) {
            CatLogger.logError("AuthReposıtory", "register", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val basarili = suspendCancellableCoroutine<Boolean> { continuation ->
                authYonetici.resetPassword(email) { sonuc ->
                    if (continuation.isActive) continuation.resume(sonuc)
                }
            }

            if (basarili) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("E-posta gönderilemedi!"))
            }
        } catch (e: Exception) {
            CatLogger.logError("AuthReposıtory", "resetPassword", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<GoogleAuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = mAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Kullanıcı bilgileri alınamadı."))

            val uid = firebaseUser.uid
            val userRef = db.collection("users").document(uid)
            val doc = userRef.get().await()

            if (doc.exists() && !doc.getString(userNameTag).isNullOrBlank()) {
                val userModel = UserModel(
                    username = doc.getString("KullaniciAdi") ?: (firebaseUser.email?.substringBefore("@") ?: ""),
                    password = ""
                ).apply {
                    id = uid
                    name = doc.getString("Ad") ?: (firebaseUser.displayName ?: "")
                    surname = doc.getString("Soyad") ?: ""
                    email = doc.getString("Email") ?: (firebaseUser.email ?: "")
                    photoUrl = doc.getString("profilFotoUrl") ?: (firebaseUser.photoUrl?.toString() ?: "")
                    bio = doc.getString("Hakkinda") ?: ""
                    followingCount = doc.getLong("TakipEdilenSayisi")
                    followersCount = doc.getLong("takipciSayisi")
                    postCount = doc.getLong("gonderiSayisi") ?: 0L
                }
                Result.success(GoogleAuthResult.ExistingUser(userModel))
            } else {
                val newUserModel = UserModel(
                    name = firebaseUser.displayName ?: "",
                    surname = "",
                    email = firebaseUser.email ?: "",
                    username = firebaseUser.email?.substringBefore("@") ?: "user_${uid.take(5)}",
                ).apply {
                    id = uid
                    photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                }

                Result.success(GoogleAuthResult.NewUser(newUserModel))
            }
        } catch (e: FirebaseNetworkException) {
            Result.failure(AuthError.NetworkError())

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(AuthError.InvalidCredential())

        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(AuthError.UserDisabled())

        } catch (e: Exception) {
            Result.failure(AuthError.Unknown())
        }
    }

}