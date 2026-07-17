package com.dpadwarrior.betterdpad.views.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpadwarrior.betterdpad.accessibility.BetterDpadAccessibilityService
import com.dpadwarrior.betterdpad.views.BetterDpadTheme
import com.dpadwarrior.betterdpad.views.main.settings.FocusHighlightAppFilterScreen
import com.dpadwarrior.betterdpad.views.main.settings.SettingsViewModel

private enum class Screen { MAIN, FOCUS_HIGHLIGHT_APP_FILTER }

class MainView : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterDpadTheme {
                val context = LocalContext.current
                var serviceEnabled by remember { mutableStateOf(false) }
                var screen by remember { mutableStateOf(Screen.MAIN) }
                val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                BackHandler(enabled = screen != Screen.MAIN) { screen = Screen.MAIN }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            serviceEnabled = isAccessibilityServiceEnabled(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                when (screen) {
                    Screen.MAIN -> MainScreen(
                        serviceEnabled = serviceEnabled,
                        onEnableClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        settingsState = settingsState,
                        onAppEnabledToggle = settingsViewModel::setAppEnabled,
                        onDebugToggle = settingsViewModel::setDebugMode,
                        onJumpToFirstChange = settingsViewModel::setJumpToFirst,
                        onJumpToLastChange = settingsViewModel::setJumpToLast,
                        onJumpToFabChange = settingsViewModel::setJumpToFab,
                        onQuickJumpChange = settingsViewModel::setQuickJump,
                        onQuickJumpHintStyleChange = settingsViewModel::setQuickJumpHintStyle,
                        onFocusHighlightToggle = settingsViewModel::setFocusHighlightEnabled,
                        onNavigateToFocusHighlightAppFilter = { screen = Screen.FOCUS_HIGHLIGHT_APP_FILTER },
                        onDpadUpChange = settingsViewModel::setDpadUp,
                        onDpadDownChange = settingsViewModel::setDpadDown,
                        onDpadLeftChange = settingsViewModel::setDpadLeft,
                        onDpadRightChange = settingsViewModel::setDpadRight,
                        onDpadSelectChange = settingsViewModel::setDpadSelect,
                        onDpadModeToggle = settingsViewModel::setDpadModeEnabled,
                        onRequestShizukuPermission = settingsViewModel::requestShizukuPermission
                    )
                    Screen.FOCUS_HIGHLIGHT_APP_FILTER -> FocusHighlightAppFilterScreen(
                        mode = settingsState.focusHighlightAppFilterMode,
                        onModeChange = settingsViewModel::setFocusHighlightAppFilterMode,
                        selectedPackages = settingsState.focusHighlightAppList,
                        onSelectedPackagesChange = settingsViewModel::setFocusHighlightAppList,
                        loadInstalledApps = settingsViewModel::loadInstalledApps,
                        onBack = { screen = Screen.MAIN }
                    )
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    var accessibilityEnabled = 0
    val service = context.packageName + "/" + BetterDpadAccessibilityService::class.java.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (_: Settings.SettingNotFoundException) {
        // accessibility is not enabled
    }
    val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            stringColonSplitter.setString(settingValue)
            while (stringColonSplitter.hasNext()) {
                if (stringColonSplitter.next().equals(service, ignoreCase = true)) return true
            }
        }
    }
    return false
}
