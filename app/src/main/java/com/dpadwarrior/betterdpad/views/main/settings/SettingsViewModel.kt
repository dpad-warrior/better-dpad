package com.dpadwarrior.betterdpad.views.main.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dpadwarrior.betterdpad.BetterDpad
import com.dpadwarrior.betterdpad.accessibility.QuickJumpHintStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as BetterDpad).preferences
    private val shizukuKeyInjector = (application as BetterDpad).shizukuKeyInjector

    private val dpadKeyCodes = combine(
        prefs.dpadUpKeyCode,
        prefs.dpadDownKeyCode,
        prefs.dpadLeftKeyCode,
        prefs.dpadRightKeyCode,
        prefs.dpadSelectKeyCode
    ) { up, down, left, right, select -> DpadKeyCodes(up, down, left, right, select) }

    val uiState = combine(
        prefs.isAppEnabled,
        prefs.isDebugModeEnabled,
        prefs.jumpToFirstKeyCode,
        prefs.jumpToLastKeyCode,
        prefs.jumpToFabKeyCode
    ) { appEnabled, debugMode, jumpToFirst, jumpToLast, jumpToFab ->
        SettingsState(appEnabled, debugMode, jumpToFirst = jumpToFirst, jumpToLast = jumpToLast, jumpToFab = jumpToFab)
    }.combine(prefs.quickJumpKeyCode) { state, quickJump ->
        state.copy(quickJump = quickJump)
    }.combine(prefs.quickJumpHintStyle) { state, quickJumpHintStyle ->
        state.copy(quickJumpHintStyle = quickJumpHintStyle)
    }.combine(prefs.isFocusHighlightEnabled) { state, focusHighlightEnabled ->
        state.copy(focusHighlightEnabled = focusHighlightEnabled)
    }.combine(prefs.isDpadModeEnabled) { state, dpadModeEnabled ->
        state.copy(dpadModeEnabled = dpadModeEnabled)
    }.combine(dpadKeyCodes) { state, codes ->
        state.copy(
            dpadUp = codes.up,
            dpadDown = codes.down,
            dpadLeft = codes.left,
            dpadRight = codes.right,
            dpadSelect = codes.select
        )
    }.combine(prefs.inputModeModifierKeyCode) { state, inputModeModifier ->
        state.copy(inputModeModifier = inputModeModifier)
    }.combine(shizukuKeyInjector.state) { state, shizukuState ->
        state.copy(shizukuState = shizukuState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setAppEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAppEnabled(enabled) }
    }

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDebugMode(enabled) }
    }

    fun setFocusHighlightEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setFocusHighlightEnabled(enabled) }
    }

    fun setDpadModeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setDpadModeEnabled(enabled) }
    }

    fun setJumpToFirst(keyCode: Int?) {
        viewModelScope.launch { prefs.setJumpToFirstKeyCode(keyCode) }
    }

    fun setJumpToLast(keyCode: Int?) {
        viewModelScope.launch { prefs.setJumpToLastKeyCode(keyCode) }
    }

    fun setJumpToFab(keyCode: Int?) {
        viewModelScope.launch { prefs.setJumpToFabKeyCode(keyCode) }
    }

    fun setQuickJump(keyCode: Int?) {
        viewModelScope.launch { prefs.setQuickJumpKeyCode(keyCode) }
    }

    fun setQuickJumpHintStyle(style: QuickJumpHintStyle) {
        viewModelScope.launch { prefs.setQuickJumpHintStyle(style) }
    }

    fun setDpadUp(keyCode: Int?) {
        viewModelScope.launch { prefs.setDpadUpKeyCode(keyCode) }
    }

    fun setDpadDown(keyCode: Int?) {
        viewModelScope.launch { prefs.setDpadDownKeyCode(keyCode) }
    }

    fun setDpadLeft(keyCode: Int?) {
        viewModelScope.launch { prefs.setDpadLeftKeyCode(keyCode) }
    }

    fun setDpadRight(keyCode: Int?) {
        viewModelScope.launch { prefs.setDpadRightKeyCode(keyCode) }
    }

    fun setDpadSelect(keyCode: Int?) {
        viewModelScope.launch { prefs.setDpadSelectKeyCode(keyCode) }
    }

    fun setInputModeModifier(keyCode: Int?) {
        viewModelScope.launch { prefs.setInputModeModifierKeyCode(keyCode) }
    }

    fun requestShizukuPermission() {
        shizukuKeyInjector.requestPermission()
    }
}

private data class DpadKeyCodes(
    val up: Int?,
    val down: Int?,
    val left: Int?,
    val right: Int?,
    val select: Int?
)
