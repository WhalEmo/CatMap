package com.beem.catmap.gonderi

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

sealed class ProfileUpdateResult {
    object Idle : ProfileUpdateResult()
    data class Success(val newPhotoUrl: String?, val newUsername: String,val newAd: String,val newSoyad: String, val newHakkinda: String) : ProfileUpdateResult()
    object UsernameAlreadyTaken : ProfileUpdateResult()
    data class Error(val message: String) : ProfileUpdateResult()
    object Loading : ProfileUpdateResult()
}

class ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Kullanıcının TÜM profil bilgilerini TEK BİR doküman okumasıyla getirir.
     */
    suspend fun getUserProfile(userId: String): UserProfileData? {
        return try {
            Log.d("PROFILE", "İstenen userId = $userId")

            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            Log.d("PROFILE", "exists = ${snapshot.exists()}")

            if (snapshot.exists()) {
                val ad = snapshot.getString("Ad") ?: ""
                val soyad = snapshot.getString("Soyad") ?: ""
                val kullaniciAdi = snapshot.getString("KullaniciAdi") ?: ""
                val fotoUrl = snapshot.getString("profilFotoUrl")
                val hakkinda = snapshot.getString("Hakkinda") ?: ""

                Log.d("PROFILE", "Getirilen -> KullaniciAdi: $kullaniciAdi, fotoUrl: $fotoUrl, Hakkinda: $hakkinda")

                UserProfileData(
                    userId = userId,
                    kullaniciAdi = kullaniciAdi,
                    ad = ad,
                    soyad = soyad,
                    fotoUrl = fotoUrl,
                    hakkinda = hakkinda
                )
            } else {
                Log.d("PROFILE", "Doküman bulunamadı.")
                null
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "getUserProfile hata", e)
            null
        }
    }

    /**
     * Tüm profil değişikliklerini Kontrol Edip Toplu Günceller
     */
    suspend fun updateFullProfile(currentUserId: String, currentUsername: String, newUsername: String, currentAd: String, newAd: String,currentSoyad: String,newSoyad: String,newHakkinda: String, newImageUri: Uri?): ProfileUpdateResult {
        return try {
            val updates = mutableMapOf<String, Any>()
            var uploadedPhotoUrl: String? = null

            if (newUsername.trim() != currentUsername.trim()) {
                val isAvailable = checkUsernameAvailability(newUsername, currentUserId)
                if (!isAvailable) {
                    return ProfileUpdateResult.UsernameAlreadyTaken
                }
                updates["KullaniciAdi"] = newUsername.trim()
            }

            if (newAd.trim() != currentAd.trim()) {
                updates["Ad"] = newAd.trim()
            }

            if (newSoyad.trim() != currentSoyad.trim()) {
                updates["Soyad"] = newSoyad.trim()
            }

            if (newImageUri != null) {
                uploadedPhotoUrl = uploadProfilePhotoToStorage(newImageUri, currentUserId)
                updates["profilFotoUrl"] = uploadedPhotoUrl
            }

            updates["Hakkinda"] = newHakkinda.trim()
            if (updates.isNotEmpty()) {
                db.collection("users")
                    .document(currentUserId)
                    .update(updates)
                    .await()
            }

            ProfileUpdateResult.Success(
                newPhotoUrl = uploadedPhotoUrl,
                newUsername = newUsername.trim(),
                newAd = newAd.trim(),
                newSoyad = newSoyad.trim(),
                newHakkinda = newHakkinda.trim()
            )

        } catch (e: Exception) {
            Log.e("PROFILE", "updateFullProfile hata", e)
            ProfileUpdateResult.Error(e.localizedMessage ?: "Profil güncellenirken bir hata oluştu.")
        }
    }
    // --- YARDIMCI METOTLAR ---

    private suspend fun uploadProfilePhotoToStorage(imageUri: Uri, userId: String): String {
        val ref = storage.reference.child("profile_images/$userId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    private suspend fun checkUsernameAvailability(username: String, currentUserId: String): Boolean {
        // Firestore'da arama yaparken 'KullaniciAdi' alan adını kullanıyoruz
        val snapshot = db.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .get()
            .await()

        return snapshot.isEmpty || snapshot.documents.all { it.id == currentUserId }
    }
}