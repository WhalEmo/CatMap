package com.beem.catmap.ui.navigation

import android.support.annotation.IdRes
import com.beem.catmap.R

enum class Screen(val tag: String, val tabIndex: Int, @IdRes val menuId: Int?, val isNode: Boolean = false) {
    MAP("MAP_FRAGMENT_TAG", 0, R.id.fragment_map, true),
    UPLOAD("YUKLE", 1, R.id.fragment_yukle, true),
    CHAT("CHAT", 2, R.id.fragment_chat, true),
    PROFILE("PROFILE", 3, R.id.fragment_profile, true),
    CAMERA("CAMERA", -1, null);

    companion object {
        fun fromTag(tag: String?): Screen = entries.find { it.tag == tag } ?: MAP
        fun fromMenuId(@IdRes id: Int): Screen = entries.find { it.menuId == id } ?: MAP
    }
}