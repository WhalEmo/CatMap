package com.beem.catmap.ui.navigation

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CatMapNavigationRenderer(
    private val activity: AppCompatActivity,
    private val containerId: Int,
    private val provider: FragmentProvider
) : DefaultLifecycleObserver {

    init {
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                SmartNavigationEngine.navigationEvents.collect { state ->
                    render(
                        targetScreen = state.screen,
                        trigger = state.trigger,
                        newArgs = state.args,
                        targetScreenId = state.screenId
                    )
                }
            }
        }
    }


    private fun render(targetScreen: Screen, trigger: NavigationTrigger, newArgs: Bundle, targetScreenId: String) {
        val fm = activity.supportFragmentManager

        if (fm.isStateSaved) return

        val transaction = fm.beginTransaction()

        val oldScreen = SmartNavigationEngine.getOldScreen() ?: SmartNavigationEngine.getCurrentScreen()
        when (trigger) {
            NavigationTrigger.INITIAL -> renderInitialAnimation(transaction)
            NavigationTrigger.FORWARD -> renderForwardAnimation(transaction, oldScreen, targetScreen)
            NavigationTrigger.BACKWARD -> renderBackwardAnimation(transaction, oldScreen, targetScreen)

        }


        val validTags = Screen.entries.map { it.tag }
        for (f in fm.fragments) {
            if (f != null && f.tag != null) {
                val baseTag = f.tag?.extractBaseTag()

                if (validTags.contains(baseTag)) {
                    if (f.tag != targetScreenId) {
                        val screen = Screen.fromTag(baseTag)
                        if (screen.isNode) {
                            transaction.hide(f)
                        } else {
                            f.view?.clearAnimation()
                            transaction.hide(f)
                        }
                    }
                }
            }
        }

        val targetFragment = fm.findFragmentByTag(targetScreenId)

        when{
            targetFragment == null -> {
                provider.createFragment(targetScreen.tag)?.let { newFragment ->
                    if (!newArgs.isEmpty) {
                        newFragment.arguments = newArgs
                    }

                    transaction.add(containerId, newFragment, targetScreenId)

                    newFragment.fragmentLog("CREATE FRAGMENT")
                }
            }
            else -> {
                if (targetFragment.isDetached) {
                    transaction.attach(targetFragment)
                }
                transaction.show(targetFragment)

                targetFragment.fragmentLog("CACHE FRAGMENT")
            }
        }

        transaction.commitAllowingStateLoss()

    }


    private fun renderInitialAnimation(transaction: FragmentTransaction) {
    }

    private fun renderForwardAnimation(transaction: FragmentTransaction, oldScreen: Screen, targetScreen: Screen) {
        if (targetScreen == Screen.CAMERA) {
            transaction.setCustomAnimations(R.anim.slide_up, R.anim.stay_still, R.anim.pop_stay_still, R.anim.slide_down)
        } else if (targetScreen.tabIndex > oldScreen.tabIndex) {
            Log.d("Yon", "Sağdan gelecem")
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            Log.d("Yon", "Soldan gelecem")
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun renderBackwardAnimation(transaction: FragmentTransaction, oldScreen: Screen, targetScreen: Screen) {
        if (oldScreen == Screen.CAMERA) {
            Log.d("Yon", "backward camera ${oldScreen.tag} - ${targetScreen.tag}")
            transaction.setCustomAnimations(
                R.anim.stay_still,
                R.anim.slide_down,
                R.anim.stay_still,
                R.anim.slide_down
            )
        } else {
            Log.d("Yon", "backward ${oldScreen.tag} - ${targetScreen.tag}")
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}