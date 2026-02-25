package com.dpadwarrior.betterdpad.views.main.settings

data class SettingsState(
    val appEnabled: Boolean = true,
    val debugMode: Boolean = false,
    val jumpToFirst: Int? = null,
    val jumpToLast: Int? = null,
    val jumpToFab: Int? = null
)
