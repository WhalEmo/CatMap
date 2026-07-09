package com.beem.catmap.ui.navigation

import android.os.Bundle
import android.util.Log
import java.util.Stack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SmartNavigationEngine {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Initial)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val backStack = Stack<Pair<Screen, String>>()
    private var uiBridge: CatMapNavigationEngine? = null

    private var currentArguments: Bundle? = null


    @get:JvmName("getInternalCurrentScreen")
    @JvmStatic
    var currentScreen: Screen = Screen.MAP
        private set

    private var oldScreen: Screen? = null

    private var currentNode: Screen = Screen.MAP

    private var onSlidingPanelCheck: (() -> Boolean)? = null
    private var onSystemExit: (() -> Unit)? = null

    @JvmStatic
    fun init(uiBridge: CatMapNavigationEngine) {
        this.uiBridge = uiBridge
        backStack.clear()
        val initialId = generateScreenId(Screen.MAP)
        backStack.push(Pair(Screen.MAP, initialId))
        currentScreen = Screen.MAP
        currentNode = Screen.MAP
        emitCurrentState(
            screen = Screen.MAP,
            trigger = NavigationTrigger.INITIAL,
            screenId = initialId,
            args = Bundle()
        )
    }

    @JvmStatic
    fun registerActivityCallbacks(
        slidingCheck: () -> Boolean,
        systemExit: () -> Unit
    ) {
        this.onSlidingPanelCheck = slidingCheck
        this.onSystemExit = systemExit
    }

    @JvmStatic
    @JvmOverloads
    fun navigateTo(targetScreen: Screen, args: Bundle? = null, key: String? = null) {
        val targetScreenId = generateScreenId(targetScreen, key)

        if (backStack.isNotEmpty() && backStack.peek().second == targetScreenId && args == null) return

        oldScreen = currentScreen

        currentArguments = args

        val targetScreenWrapper = Pair(targetScreen, targetScreenId)

        if (targetScreen.isNode){
            backStack.clear()
            backStack.push(targetScreenWrapper)
        } else {
            backStack.push(targetScreenWrapper)
        }


        currentScreen = targetScreen
        currentNode = if(currentScreen.isNode) currentScreen else currentNode
        emitCurrentState(
            screen = targetScreen,
            trigger = NavigationTrigger.FORWARD,
            screenId = targetScreenId,
            args = Bundle()
        )
    }

    @JvmStatic
    fun navigateBack() {
        val stackBefore = backStack.joinToString(separator = " -> ") { printWrapper(it) }
        Log.d("NAV_BACK_DEDEKTOR", "--- GERİ BASILDI (ÖNCE) ---")
        Log.d("NAV_BACK_DEDEKTOR", "Mevcut Ekran: $currentScreen")
        Log.d("NAV_BACK_DEDEKTOR", "Yığın Düzeni: $stackBefore")

        if (currentScreen == Screen.MAP && onSlidingPanelCheck?.invoke() == true) {
            Log.d("NAV_BACK_DEDEKTOR", "Kaptan: Sliding Panel açıktı, kapatıldı.")
            return
        }

        if (backStack.size > 1) {
            backStack.pop()
            val previousPair = backStack.peek()
            val previousScreen = previousPair.first
            val previousKey = previousPair.second
            if(previousScreen.isNode){
                backStack.clear()
                backStack.push(previousPair)
                currentNode = previousScreen
            }
            oldScreen = currentScreen
            currentScreen = previousScreen
            emitCurrentState(previousScreen, NavigationTrigger.BACKWARD, Bundle(), previousKey)
        } else {
            val defaultId = generateScreenId(currentNode, null)
            backStack.clear()
            backStack.push(Pair(currentNode, defaultId))
            oldScreen = currentScreen
            currentScreen = currentNode
            emitCurrentState(currentScreen, NavigationTrigger.INITIAL, Bundle(), defaultId)

            Log.d("NAV_BACK_DEDEKTOR", "Kaptan: Geri gidecek yer kalmadı, uygulamadan çıkılıyor.")
            onSystemExit?.invoke()
        }

        val stackAfter = backStack.joinToString(separator = " -> ") { printWrapper(it) }
        Log.d("NAV_BACK_DEDEKTOR", "--- GERİ BASILDI (SONRA) ---")
        Log.d("NAV_BACK_DEDEKTOR", "Yeni Ekran: $currentScreen")
        Log.d("NAV_BACK_DEDEKTOR", "Yeni Yığın: $stackAfter")
        Log.d("NAV_BACK_DEDEKTOR", "---------------------------")
    }

    private fun emitCurrentState(
        screen: Screen,
        trigger: NavigationTrigger,
        args: Bundle,
        screenId: String
    ) {
        uiBridge?.updateUISilently(screen)
        _navigationState.value = NavigationState
            .Active(
                screen = screen,
                trigger = trigger,
                args = args,
                screenId = screenId
            )
    }

    @JvmStatic
    fun consumeArguments(): Bundle? {
        val args = currentArguments
        currentArguments = null
        return args
    }

    @JvmStatic
    fun generateScreenId(screen: Screen, key: String? = null): String{
        return if(key.isNullOrEmpty()) screen.tag else screen.tag + "_" + key
    }


    @JvmStatic
    fun getCurrentScreen(): Screen {
        return currentScreen
    }

    fun getOldScreen(): Screen? {
        return oldScreen
    }

    private fun printWrapper(wrapper: Pair<Screen, String>): String{
        return "[${wrapper.first.name} key: ${wrapper.second}]"
    }
}