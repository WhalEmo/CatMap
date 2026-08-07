package com.beem.catmap.managers

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

/**
 * CatMap Profesyonel Çevrim İçi / Çevrim Dışı (Presence) Yöneticisi
 * App-wide lifecycle ve Firebase Native OnDisconnect mimarisi kullanır.
 */
object OnlinePresenceManager : DefaultLifecycleObserver {

    private const val TAG = "PresenceManager"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var connectedListener: ValueEventListener? = null
    private var isInitialized = false

    /**
     * Uygulama başladığında (örneğin Application sınıfında) 1 kez çağrılır.
     * Uygulamanın tüm yaşam döngüsünü dinlemeye başlar.
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        // ProcessLifecycleOwner ile uygulamanın ön plan / arka plan durumunu dinliyoruz
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "🟢 OnlinePresenceManager başarıyla başlatıldı.")
    }

    /**
     * Uygulama Ön Plana Çıktığında Otomatik Tetiklenir
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "📱 Uygulama ön plana çıktı -> Çevrim içi dinleyicisi kuruluyor.")
        setUserOnline()
    }

    /**
     * Uygulama Arka Plana Geçtiğinde Otomatik Tetiklenir
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "💤 Uygulama arka plana geçti -> Çevrim dışı durumuna geçiliyor.")
        setUserOffline()
    }

    /**
     * Kullanıcıyı Çevrim İçi Yapar ve Firebase Sunucu Bağlantısını Dinler
     */
    fun setUserOnline() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userStatusRef = database.getReference("durumlar").child(currentUserId)
        val connectedRef = database.getReference(".info/connected")

        // Eski dinleyici varsa temizle
        connectedListener?.let { connectedRef.removeEventListener(it) }

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false

                if (isConnected) {
                    Log.d(TAG, "⚡ Firebase bağlantısı sağlandı. OnDisconnect ve Online durumu işleniyor.")

                    // 1. 🚀 KRİTİK NOKTA: İnternet veya uygulama aniden koparsa Firebase Sunucusu Otomatik Çalıştırır!
                    userStatusRef.child("cevrimici").onDisconnect().setValue(false)
                    userStatusRef.child("sonGorulme").onDisconnect().setValue(ServerValue.TIMESTAMP)

                    // 2. Anlık olarak kullanıcının durumunu Çevrim İçi Yap
                    val onlineStatus = mapOf(
                        "cevrimici" to true,
                        "sonGorulme" to ServerValue.TIMESTAMP
                    )
                    userStatusRef.updateChildren(onlineStatus)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase bağlantı dinleyici hatası: ${error.message}")
            }
        }

        connectedRef.addValueEventListener(connectedListener!!)
    }

    /**
     * Kullanıcı Manuel Çıkış Yaptığında veya Uygulama Arka Plana Atıldığında Tetiklenir
     */
    fun setUserOffline() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userStatusRef = database.getReference("durumlar").child(currentUserId)

        // Dinleyiciyi kaldır
        connectedListener?.let {
            database.getReference(".info/connected").removeEventListener(it)
            connectedListener = null
        }

        val offlineStatus = mapOf(
            "cevrimici" to false,
            "sonGorulme" to ServerValue.TIMESTAMP
        )

        userStatusRef.updateChildren(offlineStatus)
    }
}