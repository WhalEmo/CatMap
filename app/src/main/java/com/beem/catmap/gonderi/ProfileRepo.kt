package com.beem.catmap.gonderi

import android.content.Context
import android.net.Uri
import android.util.Log
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.KullaniciAuth.copy
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
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

class ProfileRepository(context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userManager = CurrentUserManager.getInstance(context)

    suspend fun getUserProfile(userId: String, forceRefresh: Boolean = false): UiState<Kullanici> = withContext(Dispatchers.IO) {
        val isMyProfile = userId == UserSession.userId

        if (isMyProfile && !forceRefresh) {
            val cachedUser = userManager.getCurrentUser()
            if (!cachedUser.kullaniciAdi.isNullOrBlank()) {
                return@withContext UiState.Success(cachedUser)
            }
        }

        fetchFromFirestore(userId, isMyProfile)
    }

    private suspend fun fetchFromFirestore(userId: String, isMyProfile: Boolean): UiState<Kullanici> {
        return try {
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
                    fotoUrl = snapshot.getString("profilFotoUrl")
                    biyografi = snapshot.getString("Hakkinda").orEmpty()
                    takipciSayisi = snapshot.getLong("takipciSayisi") ?: 0L
                    takipEdilenSayisi = snapshot.getLong("TakipEdilenSayisi") ?: 0L
                    gonderiSayisi = snapshot.getLong("gonderiSayisi") ?: 0L
                }

                if (isMyProfile) {
                    updateLocalSession(profileData)
                }

                UiState.Success(profileData)
            } else {
                UiState.Error("Kullanıcı bulunamadı.")
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                UiState.Blocked
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

            if (finalAd != currentAd.trim()) {
                updates["Ad"] = finalAd
            }

            if (finalSoyad != currentSoyad.trim()) {
                updates["Soyad"] = finalSoyad
            }

            if (newImageUri != null) {
                uploadedPhotoUrl = uploadProfilePhotoToStorage(newImageUri, currentUserId)
                updates["profilFotoUrl"] = uploadedPhotoUrl
            }

            updates["Hakkinda"] = finalHakkinda
            if (updates.isNotEmpty()) {
                db.collection("users")
                    .document(currentUserId)
                    .update(updates)
                    .await()
            }

            val currentUser = userManager.getCurrentUser()
            val finalPhotoUrl = uploadedPhotoUrl ?: currentUser.fotoUrl

            val updatedUser = currentUser.copy(
                kullaniciAdi = finalUsername,
                ad = finalAd,
                soyad = finalSoyad,
                biyografi = finalHakkinda,
                fotoUrl = finalPhotoUrl
            )

            // Tek bir noktadan yerel session güncellenir
            updateLocalSession(updatedUser)

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

    private fun updateLocalSession(user: Kullanici) {
        userManager.updateProfileDetails(
            ad = user.ad.orEmpty(),
            soyad = user.soyad.orEmpty(),
            kullaniciAdi = user.kullaniciAdi.orEmpty(),
            takipci = user.takipciSayisi ?: 0L,
            takipEdilen = user.takipEdilenSayisi ?: 0L,
            gonderiSayisi = user.gonderiSayisi ?: 0L,
            biyografi = user.biyografi.orEmpty(),
            fotoUrl = user.fotoUrl
        )
        userManager.setCurrentUser(user)
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