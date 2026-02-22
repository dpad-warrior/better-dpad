package com.dpadwarrior.betterdpad.accessibility.appconfigs

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

    /**
     * Content description or text label of the app's primary FAB.
     * Override in each app config to enable FAB jump support.
     * Leave null to indicate the app has no FAB.
     */
    open val fabLabel: String? = null

    /**
     * Focuses the FAB by searching for [fabLabel] in the view tree.
     * Returns true if the node was found and focused.
     */
    fun focusFab(rootNode: AccessibilityNodeInfo): Boolean {
        val label = fabLabel ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(label)
        val focused = nodes.firstOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) == true
        nodes.forEach { it.recycle() }
        return focused
    }
}
