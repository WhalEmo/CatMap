package com.beem.catmap.data.repository

import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.model.ProfileUpdateResult
import com.beem.catmap.data.model.PublicUser
import com.beem.catmap.data.model.UserStats
import com.beem.catmap.ui.profile.common.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class ProfileRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val profileCache = LruCache<String, UserModel>(10)

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
                    cached.followersCount = stats.followerCount
                    cached.followingCount = stats.followingCount
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

    suspend fun getUserProfile(userId: String, forceRefresh: Boolean = false): UiState<UserModel> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cachedProfile = profileCache.get(userId)
            if (cachedProfile != null) {
                Log.d("ProfileRepository", "Profil Cache'ten getirildi: $userId")
                return@withContext UiState.Success(cachedProfile)
            }
        }
        Log.d("PROFILEREPOcalsııt", userId)
        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()
            Log.d("PROFILEREPOcalsııt", userId)
            if (snapshot.exists()) {
                val profileData = UserModel().apply {
                    id = userId
                    name = snapshot.getString("Ad").orEmpty()
                    surname = snapshot.getString("Soyad").orEmpty()
                    username = snapshot.getString("KullaniciAdi").orEmpty()
                    photoUrl = snapshot.getString("profilFotoUrl").orEmpty()
                    bio = snapshot.getString("Hakkinda").orEmpty()
                    followersCount = snapshot.getLong("takipciSayisi") ?: 0L
                    followingCount = snapshot.getLong("TakipEdilenSayisi") ?: 0L
                    postCount = snapshot.getLong("gonderiSayisi") ?: 0L
                }

                // RAM Cache'e kaydet
                profileCache.put(userId, profileData)

                UiState.Success(profileData)
            } else {
                UiState.Error("Kullanıcı bulunamadı.")
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.d("PROFILEREPO", e.toString())
                UiState.BlockedBy()
            } else {
                Log.d("PROFILEREPO",e.toString())
                UiState.Error(e.localizedMessage ?: "Veritabanı hatası oluştu.")
            }
        } catch (e: Exception) {
            Log.d("PROFILEREPO",e.toString())
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
            val publicUpdates = mutableMapOf<String, Any>()
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
                publicUpdates["KullaniciAdi"] = finalUsername
            }

            if (finalAd != currentAd.trim()) updates["Ad"] = finalAd
            if (finalSoyad != currentSoyad.trim()) updates["Soyad"] = finalSoyad

            updates["Hakkinda"] = finalHakkinda

            if (newImageUri != null) {
                uploadedPhotoUrl = uploadProfilePhotoToStorage(newImageUri, currentUserId)
                updates["profilFotoUrl"] = uploadedPhotoUrl
                publicUpdates["FotoUrl"] = uploadedPhotoUrl
            }

            // 1. "users" koleksiyonunu güncelle
            if (updates.isNotEmpty()) {
                db.collection("users")
                    .document(currentUserId)
                    .update(updates)
                    .await()

                profileCache.remove(currentUserId)
            }

            // 2. YENİ: "publicUsers" koleksiyonunu güncelle (Eğer Kullanıcı Adı veya Fotoğraf değiştiyse)
            if (publicUpdates.isNotEmpty()) {
                db.collection("publicUsers")
                    .document(currentUserId)
                    .set(publicUpdates, SetOptions.merge())
                    .await()
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
                newName = finalAd,
                newSurname = finalSoyad,
                newBio = finalHakkinda
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

    suspend fun getPublicUserProfile(userId: String): Result<PublicUser> = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("publicUsers")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val publicData = PublicUser(
                    id = userId,
                    username = snapshot.getString("KullaniciAdi").orEmpty(),
                    photoUrl = snapshot.getString("FotoUrl").orEmpty()
                )
                Result.success(publicData)
            } else {
                Result.failure(Exception("Public profil bulunamadı."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}