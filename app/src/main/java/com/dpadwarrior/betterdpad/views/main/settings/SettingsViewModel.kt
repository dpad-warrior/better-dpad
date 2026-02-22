package com.dpadwarrior.betterdpad.views.main.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dpadwarrior.betterdpad.BetterDpad
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as BetterDpad).preferences

    val uiState = combine(
        prefs.isDebugModeEnabled,
        prefs.jumpToFirstKeyCode,
        prefs.jumpToLastKeyCode,
        prefs.jumpToFabKeyCode
    ) { debugMode, jumpToFirst, jumpToLast, jumpToFab ->
        SettingsState(debugMode, jumpToFirst, jumpToLast, jumpToFab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDebugMode(enabled) }
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
}
