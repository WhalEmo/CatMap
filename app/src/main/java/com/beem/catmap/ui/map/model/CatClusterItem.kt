package com.beem.catmap.ui.map.model

import com.beem.catmap.data.model.CatModel
import com.beem.catmap.data.model.Kediler
import com.beem.catmap.ui.map.privacy.CatPrivacyEngine
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Harita kümeleme ve gizlilik motoru için optimize edilmiş,
 * UI ve render katmanına özel kedi modeli.
 */
data class CatClusterItem(
    val id: String,
    val name: String,
    val description: String,
    val rawLatitude: Double,
    val rawLongitude: Double,
    val photoUrl: String,
    val photoList: List<String> = emptyList(),
    val uploaderId: String = "",
    val createdAtMillis: Long = 0L,
    val city: String = "",
    val district: String = "",
    val neighborhood: String = ""
) : ClusterItem {

    private val displayPosition: LatLng by lazy {
        CatPrivacyEngine.getDeterministicFuzzyLocation(id, rawLatitude, rawLongitude)
    }

    // ClusterItem Arayüzü Uygulaması
    override fun getPosition(): LatLng = displayPosition
    override fun getTitle(): String = name
    override fun getSnippet(): String = description
    override fun getZIndex(): Float = 50.0f

}