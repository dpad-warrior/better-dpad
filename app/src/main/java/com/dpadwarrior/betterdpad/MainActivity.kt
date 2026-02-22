package com.dpadwarrior.betterdpad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterDpadTheme {
                val context = LocalContext.current
                var serviceEnabled by remember { mutableStateOf(false) }
                var debugMode by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

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

                LaunchedEffect(Unit) {
                    AppPreferences.isDebugModeEnabled(context).collect { enabled ->
                        debugMode = enabled
                    }
                }

                BetterDpadScreen(
                    serviceEnabled = serviceEnabled,
                    onEnableClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    debugMode = debugMode,
                    onDebugToggle = { enabled ->
                        scope.launch { AppPreferences.setDebugMode(context, enabled) }
                    }
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
    debugMode: Boolean,
    onDebugToggle: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Better Dpad") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Accessibility Service",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (serviceEnabled) "Status: Enabled" else "Status: Disabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!serviceEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onEnableClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Enable Service")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Debug mode")
                Switch(checked = debugMode, onCheckedChange = onDebugToggle)
            }
            HorizontalDivider()
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
        BetterDpadScreen(serviceEnabled = false, onEnableClick = {}, debugMode = false, onDebugToggle = {})
    }
}
