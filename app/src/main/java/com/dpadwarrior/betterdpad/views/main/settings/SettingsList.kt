package com.dpadwarrior.betterdpad.views.main.settings

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpadwarrior.betterdpad.views.BetterDpadTheme

@Composable
fun SettingsList(
    state: SettingsState,
    onAppEnabledToggle: (Boolean) -> Unit,
    onDebugToggle: (Boolean) -> Unit,
    onJumpToFirstChange: (Int?) -> Unit,
    onJumpToLastChange: (Int?) -> Unit,
    onJumpToFabChange: (Int?) -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text("Enable BetterDpad") },
            supportingContent = { Text("Turn on/off the helper functionalities") },
            trailingContent = { Switch(checked = state.appEnabled, onCheckedChange = onAppEnabledToggle) }
        )
        HorizontalDivider()
        Text(
            text = "Universal settings",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        KeyBindingListItem(
            label = "Jump to first element",
            keyCode = state.jumpToFirst,
            onChange = onJumpToFirstChange
        )
        HorizontalDivider()
        KeyBindingListItem(
            label = "Jump to last element",
            keyCode = state.jumpToLast,
            onChange = onJumpToLastChange
        )
        HorizontalDivider()
        KeyBindingListItem(
            label = "Jump to FAB",
            keyCode = state.jumpToFab,
            onChange = onJumpToFabChange
        )
        HorizontalDivider()
        Text(
            text = "Advanced",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ListItem(
            headlineContent = { Text("Debug mode") },
            trailingContent = { Switch(checked = state.debugMode, onCheckedChange = onDebugToggle) }
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true, name = "All disabled")
@Composable
private fun SettingsListPreviewDisabled() {
    BetterDpadTheme {
        SettingsList(
            state = SettingsState(),
            onAppEnabledToggle = {},
            onDebugToggle = {},
            onJumpToFirstChange = {},
            onJumpToLastChange = {},
            onJumpToFabChange = {}
        )
    }
}

@Preview(showBackground = true, name = "With bindings")
@Composable
private fun SettingsListPreviewWithBindings() {
    BetterDpadTheme {
        SettingsList(
            state = SettingsState(
                debugMode = true,
                jumpToFirst = AndroidKeyEvent.KEYCODE_POUND,
                jumpToLast = AndroidKeyEvent.KEYCODE_0,
                jumpToFab = AndroidKeyEvent.KEYCODE_STAR
            ),
            onAppEnabledToggle = {},
            onDebugToggle = {},
            onJumpToFirstChange = {},
            onJumpToLastChange = {},
            onJumpToFabChange = {}
        )
    }
}

@Composable
private fun KeyBindingListItem(
    label: String,
    keyCode: Int?,
    onChange: (Int?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = keyCode
                    ?.let { AndroidKeyEvent.keyCodeToString(it).removePrefix("KEYCODE_") }
                    ?: "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        KeyBindingDialog(
            currentKeyCode = keyCode,
            onDismiss = { showDialog = false },
            onConfirm = { newKeyCode ->
                onChange(newKeyCode)
                showDialog = false
            }
        )
    }
}
