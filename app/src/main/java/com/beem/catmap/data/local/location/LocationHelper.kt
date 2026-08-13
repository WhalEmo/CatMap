package com.beem.catmap.data.local.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.beem.catmap.data.model.CatAddressModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    /**
     * Enlem ve boylamdan güvenli bir şekilde adres modelini döner.
     * Hata durumunda veya servis olmaması halinde null döner, uygulamayı çökertmez.
     */
    suspend fun getFormattedAddress(latitude: Double, longitude: Double): CatAddressModel? = withContext(Dispatchers.IO) {

        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        val geocoder = Geocoder(context, Locale("tr", "TR"))

        return@withContext try {
            val rawAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 (API 33) ve üzeri için modern Callback yapısı
                getRawAddressAsync(geocoder, latitude, longitude)
            } else {
                // Eski Android sürümleri için senkron çağrı (IO thread üzerinde)
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            // Dönen adresi kendi modelimize dönüştürüp ayıklıyoruz
            rawAddress?.toCatAddressModel()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Android 13+ Callback yapısını Coroutine Suspend fonksiyonuna çeviren adaptör.
     */
    private suspend fun getRawAddressAsync(
        geocoder: Geocoder,
        lat: Double,
        lng: Double
    ): Address? = suspendCancellableCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (continuation.isActive) {
                        continuation.resume(addresses.firstOrNull())
                    }
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            })
        } else {
            continuation.resume(null)
        }
    }

    /**
     * Google'ın karmaşık Address nesnesini İl, İlçe, Mahalle olarak temizleme extension'ı.
     */
    private fun Address.toCatAddressModel(): CatAddressModel {
        // Şehir: adminArea (Örn: Konya)
        val cityName = adminArea ?: ""

        // İlçe: subAdminArea (Örn: Selçuklu)
        val districtName = subAdminArea ?: locality ?: ""

        // Mahalle / Cadde: subLocality veya thoroughfare
        val neighborhoodName = subLocality ?: thoroughfare ?: featureName ?: ""

        // Tam adres satırı
        val full = if (maxAddressLineIndex >= 0) getAddressLine(0) else "$neighborhoodName $districtName/$cityName"

        return CatAddressModel(
            city = cityName,
            district = districtName,
            neighborhood = neighborhoodName,
            fullAddress = full
        )
    }
}