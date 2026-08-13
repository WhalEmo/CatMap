package com.beem.catmap.utils

import com.beem.catmap.data.model.CatAddressModel

private fun String.toSlug(): String {
    return this.lowercase()
        .replace("ğ", "g")
        .replace("ü", "u")
        .replace("ş", "s")
        .replace("ı", "i")
        .replace("ö", "o")
        .replace("ç", "c")
        .replace(" mah.", "")
        .replace(" mahallesi", "")
        .replace(Regex("[^a-z0-9]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
}

fun formatBadgeId(city: String, district: String, neighborhood: String): String {
    val cleanCity = city.toSlug()
    val cleanDistrict = district.toSlug()
    val cleanNeighborhood = neighborhood.toSlug()

    return "${cleanCity}_${cleanDistrict}_${cleanNeighborhood}"
}

fun CatAddressModel.toFormatBadgeId(): String {
    return formatBadgeId(
        city = this.city,
        district = this.district,
        neighborhood = this.neighborhood
    )
}