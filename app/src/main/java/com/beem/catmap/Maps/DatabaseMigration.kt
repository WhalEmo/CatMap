package com.beem.catmap.Maps

import android.util.Log
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FirebaseFirestore

object DatabaseMigration {

    fun runGeoHashMigration() {
        val db = FirebaseFirestore.getInstance()

        Log.d("MIGRATION", "GeoHash veri göçü başlatıldı (Kotlin)...")

        db.collection("cats").get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                var batch = db.batch()
                var operationCount = 0
                var totalMigrated = 0

                for (doc in queryDocumentSnapshots) {
                    if (!doc.contains("geohash")) {
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")

                        // lat ve lng null değilse işleme gir (Java'daki if (lat != null) mantığının güvenli hali)
                        if (lat != null && lng != null) {
                            val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng))

                            batch.update(doc.reference, "geohash", hash)
                            operationCount++
                            totalMigrated++

                            // Firestore WriteBatch limiti (Maksimum 500)
                            if (operationCount >= 400) {
                                batch.commit()
                                batch = db.batch() // Yeni bir batch paketi aç
                                operationCount = 0
                                Log.d("MIGRATION", "400'lük bir paket başarıyla Firebase'e yazıldı.")
                            }
                        }
                    }
                }

                // Kalan son işlemleri gönder
                if (operationCount > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("MIGRATION", "Migration Kusursuz Tamamlandı. Toplam güncellenen eski kedi: $totalMigrated")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MIGRATION", "Son paket yazılırken hata: ${e.message}")
                        }
                } else {
                    Log.d("MIGRATION", "Sistem tarandı. Güncellenecek eski kedi bulunamadı, tüm veriler zaten yeni mimaride.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MIGRATION", "Kedi verileri çekilirken kritik hata: ${e.message}")
            }
    }
}