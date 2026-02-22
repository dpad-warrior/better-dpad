package com.dpadwarrior.betterdpad.settings

data class SettingsState(
    val debugMode: Boolean = false,
    val jumpToFirst: Int? = null,
    val jumpToLast: Int? = null,
    val jumpToFab: Int? = null
)
