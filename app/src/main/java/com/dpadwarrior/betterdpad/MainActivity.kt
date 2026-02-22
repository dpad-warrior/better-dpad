package com.dpadwarrior.betterdpad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dpadwarrior.betterdpad.settings.SettingsList
import com.dpadwarrior.betterdpad.settings.SettingsState
import com.dpadwarrior.betterdpad.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterDpadTheme {
                val context = LocalContext.current
                var serviceEnabled by remember { mutableStateOf(false) }
                val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

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

                BetterDpadScreen(
                    serviceEnabled = serviceEnabled,
                    onEnableClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    settingsState = settingsState,
                    onDebugToggle = settingsViewModel::setDebugMode,
                    onJumpToFirstChange = settingsViewModel::setJumpToFirst,
                    onJumpToLastChange = settingsViewModel::setJumpToLast,
                    onJumpToFabChange = settingsViewModel::setJumpToFab
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetterDpadScreen(
    serviceEnabled: Boolean,
    onEnableClick: () -> Unit,
    settingsState: SettingsState,
    onDebugToggle: (Boolean) -> Unit,
    onJumpToFirstChange: (Int?) -> Unit,
    onJumpToLastChange: (Int?) -> Unit,
    onJumpToFabChange: (Int?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("BetterDpad") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (!serviceEnabled) {
                Card(
                    onClick = onEnableClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Accessibility permission needed - click me to open settings",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            SettingsList(
                state = settingsState,
                onDebugToggle = onDebugToggle,
                onJumpToFirstChange = onJumpToFirstChange,
                onJumpToLastChange = onJumpToLastChange,
                onJumpToFabChange = onJumpToFabChange
            )
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

@Preview(showBackground = true)
@Composable
fun BetterDpadScreenPreview() {
    BetterDpadTheme {
        BetterDpadScreen(
            serviceEnabled = false,
            onEnableClick = {},
            settingsState = SettingsState(),
            onDebugToggle = {},
            onJumpToFirstChange = {},
            onJumpToLastChange = {},
            onJumpToFabChange = {}
        )
    }
}
