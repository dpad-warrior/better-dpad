package com.dpadwarrior.betterdpad.views.main.settings

import com.dpadwarrior.betterdpad.shizuku.ShizukuState

data class SettingsState(
    val appEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val focusHighlightEnabled: Boolean = false,
    val jumpToFirst: Int? = null,
    val jumpToLast: Int? = null,
    val jumpToFab: Int? = null,
    val dpadUp: Int? = null,
    val dpadDown: Int? = null,
    val dpadLeft: Int? = null,
    val dpadRight: Int? = null,
    val dpadSelect: Int? = null,
    val inputModeModifier: Int? = null,
    val shizukuState: ShizukuState = ShizukuState.UNAVAILABLE
)
