package com.beem.catmap.gonderi

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.collection.LruCache
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

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

class ProfileRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userManager = CurrentUserManager.getInstance(context)

    private val otherUserProfileCache = LruCache<String, UserProfileData>(10)

    suspend fun getUserProfile(userId: String, forceRefresh: Boolean = false): UiState<UserProfileData> {
        val isMyProfile = userId == UserSession.userId

        // 1. Kendi profilimiz ise ve yenileme istenmiyorsa CurrentUserManager'dan al
        if (isMyProfile && !forceRefresh) {
            val cachedUser = userManager.getCurrentUser()
            val profileState = userManager.profileState.value

            val localProfile = UserProfileData(
                userId = userId,
                kullaniciAdi = cachedUser.getKullaniciAdi() ?: "",
                ad = cachedUser.getAd() ?: "",
                soyad = cachedUser.getSoyad(),
                fotoUrl = cachedUser.getFotoUrl(),
                hakkinda = profileState.biyografi ?: "",
                takipciSayisi = profileState.takipciSayisi,
                takipEdilenSayisi = profileState.takipEdilenSayisi,
                gonderiSayisi = profileState.gonderiSayisi
            )

            if (localProfile.kullaniciAdi.isNotBlank()) {
                return UiState.Success(localProfile)
            }
        }

        // 2. Başka bir kullanıcının profili ise ve yenileme istenmiyorsa LruCache'den al
        if (!isMyProfile && !forceRefresh) {
            val cachedData = otherUserProfileCache.get(userId)
            if (cachedData != null) {
                return UiState.Success(cachedData)
            }
        }

        // 3. Firestore'dan TEK SEFERDE çek ve önbellekle
        return fetchAndCacheFromFirestore(userId, isMyProfile)
    }

    private suspend fun fetchAndCacheFromFirestore(userId: String, isMyProfile: Boolean): UiState<UserProfileData> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val ad = snapshot.getString("Ad") ?: ""
                val soyad = snapshot.getString("Soyad") ?: ""
                val kullaniciAdi = snapshot.getString("KullaniciAdi") ?: ""
                val fotoUrl = snapshot.getString("profilFotoUrl")
                val hakkinda = snapshot.getString("Hakkinda") ?: ""

                // Sayıları TEK BİR belgeden alıyoruz
                val takipci = snapshot.getLong("takipciSayisi") ?: 0L
                val takipEdilen = snapshot.getLong("TakipEdilenSayisi") ?: 0L
                val gonderi = snapshot.getLong("gonderiSayisi") ?: 0L

                val profileData = UserProfileData(
                    userId = userId,
                    kullaniciAdi = kullaniciAdi,
                    ad = ad,
                    soyad = soyad,
                    fotoUrl = fotoUrl,
                    hakkinda = hakkinda,
                    takipciSayisi = takipci,
                    takipEdilenSayisi = takipEdilen,
                    gonderiSayisi = gonderi
                )

                // Önbellek güncellemeleri
                if (isMyProfile) {
                    userManager.updateProfileDetails(
                        ad = ad,
                        soyad = soyad,
                        kullaniciAdi = kullaniciAdi,
                        takipci = takipci,
                        takipEdilen = takipEdilen,
                        gonderiSayisi = gonderi,
                        biyografi = hakkinda,
                        fotoUrl = fotoUrl
                    )
                } else {
                    otherUserProfileCache.put(userId, profileData)
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

    fun updateLokalUserSession(guncelKullanici: Kullanici, eskiProfileData: UserProfileData): UserProfileData {
        val yeniKullaniciAdi = guncelKullanici.kullaniciAdi?.takeIf { it.isNotBlank() } ?: eskiProfileData.kullaniciAdi
        val yeniAd = guncelKullanici.ad?.takeIf { it.isNotBlank() } ?: eskiProfileData.ad
        val yeniSoyad = guncelKullanici.soyad ?: eskiProfileData.soyad
        val yeniBio = guncelKullanici.biyografi ?: eskiProfileData.hakkinda
        val yeniFotoUrl = guncelKullanici.fotoUrl?.takeIf { it.isNotBlank() } ?: eskiProfileData.fotoUrl

        updateLocalUserManager(
            kullaniciAdi = yeniKullaniciAdi,
            ad = yeniAd,
            soyad = yeniSoyad,
            fotoUrl = yeniFotoUrl,
            hakkinda = yeniBio
        )

        return UserProfileData(
            userId = eskiProfileData.userId,
            kullaniciAdi = yeniKullaniciAdi,
            ad = yeniAd,
            soyad = yeniSoyad,
            fotoUrl = yeniFotoUrl,
            hakkinda = yeniBio
        )
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
    ): ProfileUpdateResult {
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

            val finalUsername = newUsername.trim()
            val finalAd = newAd.trim()
            val finalSoyad = newSoyad.trim()
            val finalHakkinda = newHakkinda.trim()

            // Yerel cache/UserManager güncellemesini Repository üstlenir
            updateLocalUserManager(
                kullaniciAdi = finalUsername,
                ad = finalAd,
                soyad = finalSoyad,
                fotoUrl = uploadedPhotoUrl,
                hakkinda = finalHakkinda
            )

            ProfileUpdateResult.Success(
                newPhotoUrl = uploadedPhotoUrl,
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

    private fun updateLocalUserManager(
        kullaniciAdi: String,
        ad: String,
        soyad: String?,
        fotoUrl: String?,
        hakkinda: String
    ) {
        userManager.updateBiyografi(hakkinda)
        val currentUser = userManager.getCurrentUser().apply {
            setKullaniciAdi(kullaniciAdi)
            setAd(ad)
            setSoyad(soyad ?: "")
            fotoUrl?.let { setFotoUrl(it) }
        }
        userManager.setCurrentUser(currentUser)
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