package com.beem.catmap.ui.navigation

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
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
                    render(
                        targetScreen = state.screen,
                        trigger = state.trigger,
                        newArgs = state.args,
                        targetScreenId = state.screenId
                    )
                }
            }
            .launchIn(activity.lifecycleScope)
    }


    private fun render(targetScreen: Screen, trigger: NavigationTrigger, newArgs: Bundle, targetScreenId: String) {
        val fm = activity.supportFragmentManager
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
                        transaction.setMaxLifecycle(f, androidx.lifecycle.Lifecycle.State.STARTED)
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
                    transaction.setMaxLifecycle(newFragment, androidx.lifecycle.Lifecycle.State.RESUMED)
                    newFragment.fragmentLog("CREATE FRAGMENT")
                }
            }
            else -> {
                if (targetFragment.isDetached) {
                    transaction.attach(targetFragment)
                }
                transaction.show(targetFragment)
                transaction.setMaxLifecycle(targetFragment, androidx.lifecycle.Lifecycle.State.RESUMED)

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