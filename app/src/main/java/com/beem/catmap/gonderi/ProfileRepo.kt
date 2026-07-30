package com.beem.catmap.gonderi
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.beem.catmap.Profil.ProfileCacheManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    suspend fun getProfilePhotoUrl(context: Context, kullaniciId: String): String? {
        return try {
            val snapshot = db.collection("users").document(kullaniciId).get().await()
            if (snapshot.exists()) {
                val url = snapshot.getString("profilFotoUrl")
                val photoUrl = if (!url.isNullOrEmpty()) url else ProfileCacheManager.VALUE_EMPTY
                ProfileCacheManager.saveProfileUrl(context, kullaniciId, url)
                photoUrl
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProfilePhotoUrl(imageUri: Uri, context: Context, currentUserId: String): Result<String> {
        return try {
            val storageRef = storage.reference
                .child("profilFotolari")
                .child("${UUID.randomUUID()}.jpg")

            storageRef.putFile(imageUri).await()
            val downloadUri = storageRef.downloadUrl.await()
            val url = downloadUri.toString()

            db.collection("users")
                .document(currentUserId)
                .update("profilFotoUrl", url)
                .await()

            ProfileCacheManager.saveProfileUrl(context, currentUserId, url)
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun updateUsername(
        context: Context,
        newUsername: String,
        currentUserId: String
    ): UsernameUpdateResult {
        return try {
            // 1. Kullanıcı adının benzersiz olup olmadığını kontrol et
            val querySnapshot = db.collection("users")
                .whereEqualTo("KullaniciAdi", newUsername)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                // 2. Firestore'da kullanıcı adını güncelle
                db.collection("users")
                    .document(currentUserId)
                    .update("KullaniciAdi", newUsername)
                    .await()

                // 3. SharedPreferences kaydı yap
                val sp = context.getSharedPreferences("KullaniciKayit", Context.MODE_PRIVATE)
                sp.edit {
                    putString("KullaniciAdi", newUsername)
                }

                UsernameUpdateResult.Success
            } else {
                UsernameUpdateResult.AlreadyTaken
            }
        } catch (e: Exception) {
            UsernameUpdateResult.Error(e)
        }
    }

    suspend fun getUsernameFromDb(userId: String): String? {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.getString("KullaniciAdi")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
}