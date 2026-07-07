package com.dpadwarrior.betterdpad.views.main.settings

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpadwarrior.betterdpad.accessibility.QuickJumpHintStyle
import com.dpadwarrior.betterdpad.shizuku.ShizukuState
import com.dpadwarrior.betterdpad.views.BetterDpadTheme

@Composable
fun SettingsList(
    state: SettingsState,
    onAppEnabledToggle: (Boolean) -> Unit,
    onDebugToggle: (Boolean) -> Unit,
    onJumpToFirstChange: (Int?) -> Unit,
    onJumpToLastChange: (Int?) -> Unit,
    onJumpToFabChange: (Int?) -> Unit,
    onQuickJumpChange: (Int?) -> Unit,
    onQuickJumpHintStyleChange: (QuickJumpHintStyle) -> Unit,
    onFocusHighlightToggle: (Boolean) -> Unit,
    onDpadUpChange: (Int?) -> Unit,
    onDpadDownChange: (Int?) -> Unit,
    onDpadLeftChange: (Int?) -> Unit,
    onDpadRightChange: (Int?) -> Unit,
    onDpadSelectChange: (Int?) -> Unit,
    onDpadModeToggle: (Boolean) -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text("Enable BetterDpad") },
            supportingContent = { Text("Turn on/off the helper functionalities") },
            trailingContent = { Switch(checked = state.appEnabled, onCheckedChange = onAppEnabledToggle) }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Show focus highlight") },
            supportingContent = { Text("Draws a border around the focused item, for devices whose skin hides it") },
            trailingContent = { Switch(checked = state.focusHighlightEnabled, onCheckedChange = onFocusHighlightToggle) }
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
        KeyBindingListItem(
            label = "Quick Jump",
            description = "Labels every focusable element on screen - type its key to jump straight to it",
            keyCode = state.quickJump,
            onChange = onQuickJumpChange
        )
        HorizontalDivider()
        QuickJumpHintStyleListItem(
            style = state.quickJumpHintStyle,
            onChange = onQuickJumpHintStyleChange
        )
        HorizontalDivider()
        Text(
            text = "D-pad remapping",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ListItem(
            headlineContent = { Text("Enable D-pad remapping") },
            supportingContent = { Text("Master switch for the bindings below - turn off without losing them") },
            trailingContent = { Switch(checked = state.dpadModeEnabled, onCheckedChange = onDpadModeToggle) }
        )
        HorizontalDivider()
        if (state.shizukuState == ShizukuState.READY) {
            KeyBindingListItem(
                label = "Map key to D-pad Up",
                keyCode = state.dpadUp,
                onChange = onDpadUpChange
            )
            HorizontalDivider()
            KeyBindingListItem(
                label = "Map key to D-pad Down",
                keyCode = state.dpadDown,
                onChange = onDpadDownChange
            )
            HorizontalDivider()
            KeyBindingListItem(
                label = "Map key to D-pad Left",
                keyCode = state.dpadLeft,
                onChange = onDpadLeftChange
            )
            HorizontalDivider()
            KeyBindingListItem(
                label = "Map key to D-pad Right",
                keyCode = state.dpadRight,
                onChange = onDpadRightChange
            )
            HorizontalDivider()
            KeyBindingListItem(
                label = "Map key to D-pad Select",
                keyCode = state.dpadSelect,
                onChange = onDpadSelectChange
            )
            HorizontalDivider()
        } else {
            ListItem(
                headlineContent = { Text("Requires Shizuku") },
                supportingContent = {
                    Text(
                        if (state.shizukuState == ShizukuState.NEEDS_PERMISSION) {
                            "Grant BetterDpad access to Shizuku to enable D-pad remapping"
                        } else {
                            "Install and activate Shizuku (via wireless debugging or root) to enable D-pad remapping"
                        }
                    )
                },
                trailingContent = {
                    if (state.shizukuState == ShizukuState.NEEDS_PERMISSION) {
                        TextButton(onClick = onRequestShizukuPermission) { Text("Grant") }
                    }
                }
            )
            HorizontalDivider()
        }
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
            onJumpToFabChange = {},
            onQuickJumpChange = {},
            onQuickJumpHintStyleChange = {},
            onFocusHighlightToggle = {},
            onDpadUpChange = {},
            onDpadDownChange = {},
            onDpadLeftChange = {},
            onDpadRightChange = {},
            onDpadSelectChange = {},
            onDpadModeToggle = {},
            onRequestShizukuPermission = {}
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
                focusHighlightEnabled = true,
                jumpToFirst = AndroidKeyEvent.KEYCODE_POUND,
                jumpToLast = AndroidKeyEvent.KEYCODE_0,
                jumpToFab = AndroidKeyEvent.KEYCODE_STAR,
                dpadUp = AndroidKeyEvent.KEYCODE_W,
                dpadDown = AndroidKeyEvent.KEYCODE_S,
                dpadLeft = AndroidKeyEvent.KEYCODE_A,
                dpadRight = AndroidKeyEvent.KEYCODE_D,
                dpadSelect = AndroidKeyEvent.KEYCODE_SPACE,
                quickJumpHintStyle = QuickJumpHintStyle.LETTERS,
                shizukuState = ShizukuState.READY
            ),
            onAppEnabledToggle = {},
            onDebugToggle = {},
            onJumpToFirstChange = {},
            onJumpToLastChange = {},
            onJumpToFabChange = {},
            onQuickJumpChange = {},
            onQuickJumpHintStyleChange = {},
            onFocusHighlightToggle = {},
            onDpadUpChange = {},
            onDpadDownChange = {},
            onDpadLeftChange = {},
            onDpadRightChange = {},
            onDpadSelectChange = {},
            onDpadModeToggle = {},
            onRequestShizukuPermission = {}
        )
    }
}

@Preview(showBackground = true, name = "Shizuku needs permission")
@Composable
private fun SettingsListPreviewShizukuNeedsPermission() {
    BetterDpadTheme {
        SettingsList(
            state = SettingsState(shizukuState = ShizukuState.NEEDS_PERMISSION),
            onAppEnabledToggle = {},
            onDebugToggle = {},
            onJumpToFirstChange = {},
            onJumpToLastChange = {},
            onJumpToFabChange = {},
            onQuickJumpChange = {},
            onQuickJumpHintStyleChange = {},
            onFocusHighlightToggle = {},
            onDpadUpChange = {},
            onDpadDownChange = {},
            onDpadLeftChange = {},
            onDpadRightChange = {},
            onDpadSelectChange = {},
            onDpadModeToggle = {},
            onRequestShizukuPermission = {}
        )
    }
}

@Composable
private fun KeyBindingListItem(
    label: String,
    keyCode: Int?,
    onChange: (Int?) -> Unit,
    description: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = description?.let { { Text(it) } },
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

private val QuickJumpHintStyle.label: String
    get() = when (this) {
        QuickJumpHintStyle.NUMBERS -> "Numbers"
        QuickJumpHintStyle.LETTERS -> "Letters"
    }

@Composable
private fun QuickJumpHintStyleListItem(
    style: QuickJumpHintStyle,
    onChange: (QuickJumpHintStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = { Text("Quick Jump hint style") },
            supportingContent = {
                Text("Numbers (0-9, 10 per page) or letters (A-Z, 26 per page) - letters suit QWERTY hardware keyboards better")
            },
            trailingContent = {
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            QuickJumpHintStyle.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
