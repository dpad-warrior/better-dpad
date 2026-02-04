package com.dpadwarrior.betterdpad

import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

abstract class AppAccessibilityConfig {

    abstract val packageName: String

    open fun onAccessibilityEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ) {}

    open fun onKeyEvent(
        event: KeyEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean = false
}
