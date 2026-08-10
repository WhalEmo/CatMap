package com.beem.catmap.gonderi

import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class ProfileUpdateResult {
    object Idle : ProfileUpdateResult()
    data class Success(
        val newPhotoUrl: String?,
        val newUsername: String,
        val newAd: String,
        val newSoyad: String,
        val newHakkinda: String
    ) : ProfileUpdateResult()
    object UsernameAlreadyTaken : ProfileUpdateResult()
    data class Error(val message: String) : ProfileUpdateResult()
    object Loading : ProfileUpdateResult()
}
data class UserStats(
    val followerCount: Long = 0L,
    val followingCount: Long = 0L,
)

class ProfileRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val profileCache = LruCache<String, Kullanici>(10)

    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileRepository().also {
                    INSTANCE = it
                }
            }
        }
    }

    suspend fun getUserStats(userId: String): Result<UserStats> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val stats = UserStats(
                    followerCount = snapshot.getLong("takipciSayisi") ?: 0L,
                    followingCount = snapshot.getLong("TakipEdilenSayisi") ?: 0L,
                )

                profileCache.get(userId)?.let { cached ->
                    cached.takipciSayisi = stats.followerCount
                    cached.takipEdilenSayisi = stats.followingCount
                    profileCache.put(userId, cached)
                }

                Result.success(stats)
            } else {
                Result.failure(Exception("Kullanıcı dokümanı bulunamadı."))
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "getUserStats hatası", e)
            Result.failure(e)
        }
    }
    suspend fun getUserProfile(userId: String, forceRefresh: Boolean = false): UiState<Kullanici> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cachedProfile = profileCache.get(userId)
            if (cachedProfile != null) {
                Log.d("ProfileRepository", "Profil Cache'ten getirildi: $userId")
                return@withContext UiState.Success(cachedProfile)
            }
        }

        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val profileData = Kullanici().apply {
                    id = userId
                    ad = snapshot.getString("Ad").orEmpty()
                    soyad = snapshot.getString("Soyad").orEmpty()
                    kullaniciAdi = snapshot.getString("KullaniciAdi").orEmpty()
                    fotoUrl = snapshot.getString("profilFotoUrl").orEmpty()
                    biyografi = snapshot.getString("Hakkinda").orEmpty()
                    takipciSayisi = snapshot.getLong("takipciSayisi") ?: 0L
                    takipEdilenSayisi = snapshot.getLong("TakipEdilenSayisi") ?: 0L
                    gonderiSayisi = snapshot.getLong("gonderiSayisi") ?: 0L
                }

                // RAM Cache'e kaydet
                profileCache.put(userId, profileData)

                UiState.Success(profileData)
            } else {
                UiState.Error("Kullanıcı bulunamadı.")
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                UiState.BlockedBy
            } else {
                UiState.Error(e.localizedMessage ?: "Veritabanı hatası oluştu.")
            }
        } catch (e: Exception) {
            UiState.Error(e.localizedMessage ?: "Bilinmeyen bir hata oluştu.")
        }
    }

    suspend fun updateFullProfile(
        currentUserId: String,
        currentUsername: String,
        newUsername: String,
        currentAd: String,
        newAd: String,
        currentSoyad: String,
        newSoyad: String,
        newHakkinda: String,
        newImageUri: Uri?
    ): ProfileUpdateResult = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>()
            var uploadedPhotoUrl: String? = null

            val finalUsername = newUsername.trim()
            val finalAd = newAd.trim()
            val finalSoyad = newSoyad.trim()
            val finalHakkinda = newHakkinda.trim()

            if (finalUsername != currentUsername.trim()) {
                val isAvailable = checkUsernameAvailability(finalUsername, currentUserId)
                if (!isAvailable) {
                    return@withContext ProfileUpdateResult.UsernameAlreadyTaken
                }
                updates["KullaniciAdi"] = finalUsername
            }

            if (finalAd != currentAd.trim()) updates["Ad"] = finalAd
            if (finalSoyad != currentSoyad.trim()) updates["Soyad"] = finalSoyad

            updates["Hakkinda"] = finalHakkinda


            if (newImageUri != null) {
                uploadedPhotoUrl = uploadProfilePhotoToStorage(newImageUri, currentUserId)
                updates["profilFotoUrl"] = uploadedPhotoUrl
            }

            if (updates.isNotEmpty()) {
                db.collection("users")
                    .document(currentUserId)
                    .update(updates)
                    .await()

                profileCache.remove(currentUserId)
            }

            val finalPhotoUrl = if (uploadedPhotoUrl != null) {
                uploadedPhotoUrl
            } else {

                val documentSnapshot = db.collection("users").document(currentUserId).get().await()
                documentSnapshot.getString("profilFotoUrl")
            }

            ProfileUpdateResult.Success(
                newPhotoUrl = finalPhotoUrl,
                newUsername = finalUsername,
                newAd = finalAd,
                newSoyad = finalSoyad,
                newHakkinda = finalHakkinda
            )
        } catch (e: Exception) {
            Log.e("PROFILE", "updateFullProfile hata", e)
            ProfileUpdateResult.Error(e.localizedMessage ?: "Profil güncellenirken bir hata oluştu.")
        }
    }

    private suspend fun uploadProfilePhotoToStorage(imageUri: Uri, userId: String): String {
        val ref = storage.reference.child("profile_images/$userId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun checkUsernameAvailability(username: String, currentUserId: String): Boolean {
        val snapshot = db.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .get()
            .await()

        return snapshot.isEmpty || snapshot.documents.all { it.id == currentUserId }
    }

}