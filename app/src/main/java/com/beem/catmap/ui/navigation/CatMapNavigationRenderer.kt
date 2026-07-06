package com.beem.catmap.ui.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.beem.catmap.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CatMapNavigationRenderer(
    private val activity: AppCompatActivity,
    private val containerId: Int,
    private val provider: FragmentProvider
) : DefaultLifecycleObserver {

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        SmartNavigationEngine.navigationState
            .onEach { state ->
                if (state is NavigationState.Active) {
                    render(state.screen, state.trigger)
                }
            }
            .launchIn(activity.lifecycleScope)
    }

    private fun render(targetScreen: Screen, trigger: NavigationTrigger) {
        val fm = activity.supportFragmentManager
        val transaction = fm.beginTransaction()

        val oldScreen = SmartNavigationEngine.getCurrentScreen()

        // 🎯 DÜZELTİLDİ: Tüm animasyon kararlarını trigger'a göre alt metotlara devrettik usta!
        when (trigger) {
            NavigationTrigger.INITIAL -> renderInitialAnimation(transaction)
            NavigationTrigger.FORWARD -> renderForwardAnimation(transaction, oldScreen, targetScreen)
            NavigationTrigger.BACKWARD -> renderBackwardAnimation(transaction, oldScreen, targetScreen)
        }

        // 🛡️ Node Koruma ve Detay Ekran Temizliği
        val validTags = Screen.entries.map { it.tag }
        for (f in fm.fragments) {
            if (f != null && f.tag != null && validTags.contains(f.tag)) {
                val screen = Screen.fromTag(f.tag)
                if (screen.isNode) {
                    if (f.tag != targetScreen.tag) {
                        transaction.hide(f)
                    }
                } else {
                    transaction.remove(f)
                }
            }
        }

        var targetFragment = fm.findFragmentByTag(targetScreen.tag)
        if (targetFragment == null) {
            targetFragment = provider.createFragment(targetScreen.tag)
            if (targetFragment != null) {
                transaction.add(containerId, targetFragment, targetScreen.tag)
            }
        } else {
            transaction.show(targetFragment)
        }

        transaction.commitAllowingStateLoss()
    }

    private fun renderInitialAnimation(transaction: FragmentTransaction) {
        // İlk açılışta animasyona gerek yok, harita direkt sahneye asilce otursun
    }

    private fun renderForwardAnimation(transaction: FragmentTransaction, oldScreen: Screen, targetScreen: Screen) {
        if (targetScreen == Screen.CAMERA) {
            transaction.setCustomAnimations(R.anim.slide_up, R.anim.stay_still, R.anim.pop_stay_still, R.anim.slide_down)
        } else if (targetScreen.tabIndex > oldScreen.tabIndex) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun renderBackwardAnimation(transaction: FragmentTransaction, oldScreen: Screen, targetScreen: Screen) {
        if (oldScreen == Screen.CAMERA) {
            transaction.setCustomAnimations(R.anim.stay_still, R.anim.slide_down)
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}