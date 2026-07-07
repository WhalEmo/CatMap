package com.beem.catmap.ui.navigation

import java.util.Stack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SmartNavigationEngine {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Initial)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val backStack = Stack<Screen>()
    private var uiBridge: CatMapNavigationEngine? = null



    @get:JvmName("getInternalCurrentScreen")
    @JvmStatic
    var currentScreen: Screen = Screen.MAP
        private set

    private var oldScreen: Screen? = null

    private var currentNode: Screen = Screen.MAP

    @JvmStatic
    fun init(uiBridge: CatMapNavigationEngine) {
        this.uiBridge = uiBridge
        backStack.clear()
        backStack.push(Screen.MAP)
        currentScreen = Screen.MAP
        currentNode = Screen.MAP
        emitCurrentState(Screen.MAP, NavigationTrigger.INITIAL)
    }

    @JvmStatic
    fun navigateTo(targetScreen: Screen) {
        if (currentScreen == targetScreen) return

        oldScreen = currentScreen

        if (targetScreen.isNode){
            backStack.clear()
            backStack.push(targetScreen)
        } else {
            backStack.push(targetScreen)
        }


        currentScreen = targetScreen
        currentNode = if(currentScreen.isNode) currentScreen else currentNode
        emitCurrentState(targetScreen, NavigationTrigger.FORWARD)
    }

    @JvmStatic
    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.pop()
            val previous = backStack.peek()
            if(previous.isNode){
                backStack.clear()
                backStack.push(previous)
                currentNode = previous
            }
            oldScreen = currentScreen
            currentScreen = previous
            emitCurrentState(previous, NavigationTrigger.BACKWARD)
        } else {
            backStack.clear()
            backStack.push(currentNode)
            oldScreen = currentScreen
            currentScreen = currentNode
            emitCurrentState(currentScreen, NavigationTrigger.INITIAL)
        }
    }

    private fun emitCurrentState(screen: Screen, trigger: NavigationTrigger) {
        uiBridge?.updateUISilently(screen)
        _navigationState.value = NavigationState.Active(screen, trigger)
    }


    @JvmStatic
    fun getCurrentScreen(): Screen {
        return currentScreen
    }

    fun getOldScreen(): Screen? {
        return oldScreen
    }
}