package com.beem.catmap.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beem.catmap.data.local.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class BanSniperViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var banListener: ListenerRegistration? = null

    private val userSession = UserSession

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            startSniper(user.uid)
        } else {
            resetAndStop()
        }
    }

    private val _isBanned = MutableLiveData(userSession.isBanned)
    val isBanned: LiveData<Boolean> get() = _isBanned


    init {
        auth.addAuthStateListener(authListener)
    }

    fun startSniper(userId: String?) {
        if (userId.isNullOrBlank()) return

        // Eski pusuyu temizle
        banListener?.remove()

        // Hedefin dökümanına sızıp canlı dinlemeye başla
        banListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val banned = snapshot.getBoolean("isBanned") ?: false

                _isBanned.postValue(banned)
            }
    }

    private fun resetAndStop() {
        banListener?.remove()
        banListener = null
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        banListener?.remove()
    }
}