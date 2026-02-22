package com.dpadwarrior.betterdpad.settings

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.dpadwarrior.betterdpad.BetterDpadAccessibilityService

@Composable
fun KeyBindingDialog(
    currentKeyCode: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var capturedKeyCode by remember { mutableStateOf(currentKeyCode) }
    var isListening by remember { mutableStateOf(true) }
    val listenFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    DisposableEffect(Unit) {
        BetterDpadAccessibilityService.isCapturingKey = true
        onDispose { BetterDpadAccessibilityService.isCapturingKey = false }
    }

    LaunchedEffect(isListening) {
        if (isListening) listenFocusRequester.requestFocus()
        else confirmFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set key binding") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Press any key on your remote or keyboard.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) isListening = true }
                        .focusRequester(listenFocusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (isListening && event.type == KeyEventType.KeyDown) {
                                val keyCode = event.nativeKeyEvent.keyCode
                                if (keyCode == AndroidKeyEvent.KEYCODE_BACK) {
                                    capturedKeyCode = null
                                } else {
                                    capturedKeyCode = keyCode
                                    isListening = false
                                }
                                true
                            } else false
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = capturedKeyCode
                            ?.let { AndroidKeyEvent.keyCodeToString(it).removePrefix("KEYCODE_") }
                            ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (capturedKeyCode != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(capturedKeyCode) },
                modifier = Modifier.focusRequester(confirmFocusRequester)
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
