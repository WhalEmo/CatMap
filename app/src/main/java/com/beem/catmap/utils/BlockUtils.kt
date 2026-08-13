package com.beem.catmap.utils

object BlockUtils {
    /**
     * İki kullanıcı ID'sini alfabetik sıraya sokarak tek bir 'block_relations' anahtarı üretir.
     * Örn: generateRelationKey("B_UID", "A_UID") -> "A_UID_B_UID"
     */
    fun generateRelationKey(uid1: String, uid2: String): String {
        return if (uid1 < uid2) {
            "${uid1}_$uid2"
        } else {
            "${uid2}_$uid1"
        }
    }
}