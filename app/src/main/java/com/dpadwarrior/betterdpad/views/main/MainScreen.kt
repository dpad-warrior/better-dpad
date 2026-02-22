package com.dpadwarrior.betterdpad.views.main

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpadwarrior.betterdpad.views.BetterDpadTheme
import com.dpadwarrior.betterdpad.views.main.settings.SettingsList
import com.dpadwarrior.betterdpad.views.main.settings.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BetterDpadTheme {
        MainScreen(
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
