package com.beem.catmap.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.beem.catmap.R

object SmartNavigationEngine {

    private var fragmentManager: FragmentManager? = null
    private var containerId: Int = 0
    @JvmStatic var currentFragmentTag: String? = "MAP_FRAGMENT_TAG"
        private set

    private val tabIndices = mapOf(
        "MAP_FRAGMENT_TAG" to 0,
        "YUKLE" to 1,
        "CHAT" to 2,
        "PROFILE" to 3
    )
    private val specialIndic = "CAMERA"

    @JvmStatic
    fun init(fragmentManager: FragmentManager, containerId: Int) {
        this.fragmentManager = fragmentManager
        this.containerId = containerId
        this.currentFragmentTag = "MAP_FRAGMENT_TAG"
    }

    @JvmStatic
    fun navigateTo(tag: String, provider: FragmentProvider, onTransitionComplete: (Fragment) -> Unit) {
        val fm = fragmentManager ?: return
        val transaction = fm.beginTransaction()

        val oldTag = currentFragmentTag
        if (oldTag != null && oldTag != tag) {
            val currentIndex = tabIndices[oldTag] ?: 0
            val targetIndex = tabIndices[tag] ?: 0

            if(specialIndic.equals(tag)){
                transaction.setCustomAnimations(
                    R.anim.slide_up,
                    R.anim.stay_still,
                    R.anim.pop_stay_still,
                    R.anim.slide_down
                )
            } else if (targetIndex > currentIndex) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
            }
        }

        for (f in fm.fragments) {
            if (f != null && f.tag != null && f.tag != tag) {
                transaction.hide(f)
            }
        }

        var targetFragment = fm.findFragmentByTag(tag)

        if (targetFragment == null) {
            targetFragment = provider.createFragment(tag)
            if (targetFragment != null) {
                transaction.add(containerId, targetFragment, tag)
            }
        } else {
            transaction.show(targetFragment)
        }

        transaction.commitAllowingStateLoss()
        currentFragmentTag = tag

        targetFragment?.let { onTransitionComplete.invoke(it) }
    }

    @JvmStatic
    fun clearEngine() {
        fragmentManager = null
        currentFragmentTag = null
    }

    fun getCurrentTag(): String? = currentFragmentTag
}