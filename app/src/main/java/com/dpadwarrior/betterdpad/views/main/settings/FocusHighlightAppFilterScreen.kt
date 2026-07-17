package com.dpadwarrior.betterdpad.views.main.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.dpadwarrior.betterdpad.accessibility.FocusHighlightAppFilterMode

/**
 * Dedicated screen (not a dialog) for scoping the focus highlight to specific apps, since the
 * installed-app list can be long - opened from the "Show focus highlight" row in [SettingsList].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHighlightAppFilterScreen(
    mode: FocusHighlightAppFilterMode,
    onModeChange: (FocusHighlightAppFilterMode) -> Unit,
    selectedPackages: Set<String>,
    onSelectedPackagesChange: (Set<String>) -> Unit,
    loadInstalledApps: suspend () -> List<InstalledApp>,
    onBack: () -> Unit
) {
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }

    LaunchedEffect(Unit) {
        apps = loadInstalledApps()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus highlight apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            FocusHighlightAppFilterModeListItem(mode = mode, onModeChange = onModeChange)
            HorizontalDivider()

            val currentApps = apps
            if (currentApps == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(currentApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in selectedPackages
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = { Text(app.packageName) },
                            leadingContent = {
                                Image(
                                    bitmap = remember(app.icon) { app.icon.toBitmap().asImageBitmap() },
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        onSelectedPackagesChange(
                                            if (isChecked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                        )
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                onSelectedPackagesChange(
                                    if (checked) selectedPackages - app.packageName else selectedPackages + app.packageName
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private val FocusHighlightAppFilterMode.label: String
    get() = when (this) {
        FocusHighlightAppFilterMode.ALL_EXCEPT_SELECTED -> "All apps except selected"
        FocusHighlightAppFilterMode.ONLY_SELECTED -> "Only selected apps"
    }

@Composable
private fun FocusHighlightAppFilterModeListItem(
    mode: FocusHighlightAppFilterMode,
    onModeChange: (FocusHighlightAppFilterMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ListItem(
            headlineContent = { Text("Filter mode") },
            supportingContent = { Text("Which apps the highlight applies to") },
            trailingContent = {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FocusHighlightAppFilterMode.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onModeChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
