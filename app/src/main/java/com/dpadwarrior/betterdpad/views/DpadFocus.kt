package com.dpadwarrior.betterdpad.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

/**
 * A stronger D-pad focus indicator than the platform default (which several OEM skins suppress
 * outright - the same reason the focus-highlight overlay exists). When the wrapped control gains
 * focus it gets a filled tint plus a 2dp primary border in a pill shape. The 4dp padding is
 * constant whether focused or not, so focus never shifts layout.
 */
fun Modifier.dpadFocusHighlight(): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val borderColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val shape = RoundedCornerShape(percent = 50)

    onFocusChanged { focused = it.isFocused }
        .then(
            if (focused) {
                Modifier
                    .background(fillColor, shape)
                    .border(2.dp, borderColor, shape)
            } else {
                Modifier
            }
        )
        .padding(4.dp)
}
