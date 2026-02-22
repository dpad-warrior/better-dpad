package com.dpadwarrior.betterdpad.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dpadwarrior.betterdpad.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = combine(
        AppPreferences.isDebugModeEnabled(application),
        AppPreferences.getJumpToFirstKeyCode(application),
        AppPreferences.getJumpToLastKeyCode(application),
        AppPreferences.getJumpToFabKeyCode(application)
    ) { debugMode, jumpToFirst, jumpToLast, jumpToFab ->
        SettingsState(debugMode, jumpToFirst, jumpToLast, jumpToFab)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch { AppPreferences.setDebugMode(getApplication(), enabled) }
    }

    fun setJumpToFirst(keyCode: Int?) {
        viewModelScope.launch { AppPreferences.setJumpToFirstKeyCode(getApplication(), keyCode) }
    }

    fun setJumpToLast(keyCode: Int?) {
        viewModelScope.launch { AppPreferences.setJumpToLastKeyCode(getApplication(), keyCode) }
    }

    fun setJumpToFab(keyCode: Int?) {
        viewModelScope.launch { AppPreferences.setJumpToFabKeyCode(getApplication(), keyCode) }
    }
}
