package com.beem.catmap.ui.map.privacy

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.cos
import kotlin.math.max
import kotlin.random.Random

object CatPrivacyEngine {
    private const val MIN_ZONE_RADIUS_METERS = 120.0
    private const val MAX_ZONE_RADIUS_METERS = 300.0

    /**
     * Kümedeki noktaları kapsayan koruma çemberinin merkezini ve yarıçapını hesaplar.
     */
    fun calculateZone(points: Collection<LatLng>): Pair<LatLng, Double> {
        if (points.isEmpty()) return Pair(LatLng(0.0, 0.0), MIN_ZONE_RADIUS_METERS)
        if (points.size == 1) return Pair(points.first(), MIN_ZONE_RADIUS_METERS)

        var sumLat = 0.0
        var sumLng = 0.0
        points.forEach {
            sumLat += it.latitude
            sumLng += it.longitude
        }
        val center = LatLng(sumLat / points.size, sumLng / points.size)

        var maxDistance = 0.0
        for (point in points) {
            val dist = SphericalUtil.computeDistanceBetween(center, point)
            if (dist > maxDistance) {
                maxDistance = dist
            }
        }

        // Kedilerin sınır çizgisine denk gelmemesi için +40m güvenlik tamponu
        val calculatedRadius = max(MIN_ZONE_RADIUS_METERS, maxDistance + 40.0)
            .coerceAtMost(MAX_ZONE_RADIUS_METERS)

        return Pair(center, calculatedRadius)
    }

    /**
     * Tekil kedi pini çizilmek zorunda kaldığında tam koordinatı saptırır.
     */
    fun getFuzzyLocation(raw: LatLng, jitterMeters: Double = 75.0): LatLng {
        val angle = Random.nextDouble() * 2 * Math.PI
        val distance = Random.nextDouble(40.0, jitterMeters)

        val deltaLat = (distance * kotlin.math.sin(angle)) / 111111.0
        val deltaLng = (distance * cos(angle)) / (111111.0 * cos(Math.toRadians(raw.latitude)))

        return LatLng(raw.latitude + deltaLat, raw.longitude + deltaLng)
    }

    fun getDeterministicFuzzyLocation(catId: String, realLat: Double, realLng: Double): LatLng {
        val seed = catId.hashCode().toLong()
        val random = kotlin.random.Random(seed)

        // 80m - 150m arası sabit bir sapma mesafesi ve rastgele bir açı
        val distanceMeters = 80.0 + (random.nextDouble() * 70.0) // 80 - 150 metre
        val angle = random.nextDouble() * 2 * Math.PI

        val deltaLat = (distanceMeters * kotlin.math.sin(angle)) / 111111.0
        val deltaLng = (distanceMeters * kotlin.math.cos(angle)) / (111111.0 * kotlin.math.cos(Math.toRadians(realLat)))

        return LatLng(realLat + deltaLat, realLng + deltaLng)
    }
}