package com.beem.catmap.ui.navigation

import androidx.fragment.app.Fragment

interface FragmentProvider {
    fun createFragment(tag: String): Fragment?
}