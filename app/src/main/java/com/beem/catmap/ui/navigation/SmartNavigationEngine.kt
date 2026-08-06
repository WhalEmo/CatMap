package com.beem.catmap.ui.navigation

import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.Stack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object SmartNavigationEngine {

    private val _navigationEvents = MutableSharedFlow<NavigationState.Active>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val navigationEvents: SharedFlow<NavigationState.Active> = _navigationEvents.asSharedFlow()

    private val backStack = ArrayDeque<NavBackStackEntry>()
    private var uiBridge: CatMapNavigationEngine? = null

    @get:JvmName("getInternalCurrentScreen")
    @JvmStatic
    var currentScreen: Screen = Screen.MAP
        private set

    private lateinit var currentStack: NavBackStackEntry

    private var oldScreen: Screen? = null

    private var currentNode: Screen = Screen.MAP

    private var onSlidingPanelCheck: (() -> Boolean)? = null
    private var onSystemExit: (() -> Unit)? = null


    init {
        Log.d("NAV_ENGINE", "Kaptan: Uyandık.")
    }

    @JvmStatic
    fun init(uiBridge: CatMapNavigationEngine, initialScreen: Screen) {
        this.uiBridge = uiBridge
        backStack.clear()
        val initialId = generateScreenId(initialScreen)
        val initialEntry = NavBackStackEntry(
            screenId = initialId,
            screen = initialScreen,
            args = Bundle(),
            trigger = NavigationTrigger.INITIAL
        )
        backStack.addLast(initialEntry)
        currentScreen = initialScreen
        currentNode = initialScreen
        emitCurrentState(initialEntry)
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

        if (backStack.isNotEmpty() && backStack.last().screenId == targetScreenId && args == null) return

        oldScreen = currentScreen

        val newEntry = NavBackStackEntry(
            screen = targetScreen,
            screenId = targetScreenId,
            args = args ?: Bundle(),
            trigger = NavigationTrigger.FORWARD
        )

        if (targetScreen.isNode){
            backStack.clear()
            backStack.addLast(newEntry)
        } else {
            backStack.addLast(newEntry)
        }


        currentScreen = targetScreen
        currentNode = if(currentScreen.isNode) currentScreen else currentNode
        emitCurrentState(newEntry)
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
            backStack.removeLast()
            val previousEntry = backStack.last().copy(trigger = NavigationTrigger.BACKWARD)

            if(previousEntry.screen.isNode){
                backStack.clear()
                backStack.addLast(previousEntry)
                currentNode = previousEntry.screen
            }
            oldScreen = currentScreen
            currentScreen = previousEntry.screen
            emitCurrentState(previousEntry)
        } else {

            if (currentScreen == Screen.MAP) {
                Log.d("NAV_ENGINE", "Kaptan: Haritadayız ve yığın boş. Çıkış diyalogu tetiklenebilir.")
                onSystemExit?.invoke()

            } else {
                Log.d("NAV_ENGINE", "Kaptan: Son elemandan geri basıldı, Haritaya (MAP) dönülüyor.")

                backStack.clear()
                val mapEntry = NavBackStackEntry(
                    screen = Screen.MAP,
                    screenId = generateScreenId(Screen.MAP, null),
                    args = Bundle(),
                    trigger = NavigationTrigger.BACKWARD
                )
                backStack.addLast(mapEntry)

                oldScreen = currentScreen
                currentScreen = Screen.MAP
                currentNode = Screen.MAP

                emitCurrentState(mapEntry)
            }
        }

        val stackAfter = backStack.joinToString(separator = " -> ") { printWrapper(it) }
        Log.d("NAV_BACK_DEDEKTOR", "--- GERİ BASILDI (SONRA) ---")
        Log.d("NAV_BACK_DEDEKTOR", "Yeni Ekran: $currentScreen")
        Log.d("NAV_BACK_DEDEKTOR", "Yeni Yığın: $stackAfter")
        Log.d("NAV_BACK_DEDEKTOR", "---------------------------")
    }

    private fun emitCurrentState(entry: NavBackStackEntry) {
        uiBridge?.updateUISilently(entry.screen)
        currentStack = entry
        _navigationEvents.tryEmit(
            NavigationState.Active(
                screen = entry.screen,
                trigger = entry.trigger,
                args = entry.args,
                screenId = entry.screenId
            )
        )
    }


    @JvmStatic
    fun generateScreenId(screen: Screen, key: String? = null): String{
        return if(key.isNullOrEmpty()) screen.tag else screen.tag + "_" + key
    }


    @JvmStatic
    fun getCurrentScreen(): Screen {
        return currentScreen
    }

    fun getCurrentStack(): NavBackStackEntry {
        return currentStack
    }

    fun getOldScreen(): Screen? {
        return oldScreen
    }

    private fun printWrapper(wrapper: NavBackStackEntry): String{
        return "[--${wrapper.screenId}]"
    }
}