package com.dpadwarrior.betterdpad.views.main.settings

import com.dpadwarrior.betterdpad.accessibility.FocusHighlightAppFilterMode
import com.dpadwarrior.betterdpad.accessibility.QuickJumpHintStyle
import com.dpadwarrior.betterdpad.shizuku.ShizukuState

data class SettingsState(
    val appEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val focusHighlightEnabled: Boolean = false,
    val focusHighlightAppFilterMode: FocusHighlightAppFilterMode = FocusHighlightAppFilterMode.ALL_EXCEPT_SELECTED,
    val focusHighlightAppList: Set<String> = emptySet(),
    val dpadModeEnabled: Boolean = true,
    val jumpToFirst: Int? = null,
    val jumpToLast: Int? = null,
    val jumpToFab: Int? = null,
    val quickJump: Int? = null,
    val quickJumpHintStyle: QuickJumpHintStyle = QuickJumpHintStyle.NUMBERS,
    val dpadUp: Int? = null,
    val dpadDown: Int? = null,
    val dpadLeft: Int? = null,
    val dpadRight: Int? = null,
    val dpadSelect: Int? = null,
    val shizukuState: ShizukuState = ShizukuState.UNAVAILABLE
)
