package com.dpadwarrior.betterdpad

import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

class GoogleMapsConfig : AppAccessibilityConfig() {

    enum class Screen {
        MAIN_SCREEN,
        UNKNOWN
    }

    override val packageName: String = "com.google.android.apps.maps"

    fun detectScreen(rootNode: AccessibilityNodeInfo): Screen {
        if (hasNodeWithText(rootNode, "Search here")) return Screen.MAIN_SCREEN
        return Screen.UNKNOWN
    }

    override fun onKeyEvent(
        event: KeyEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        if (rootNode == null) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (detectScreen(rootNode)) {
                Screen.MAIN_SCREEN -> {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_0 -> {
                            val nodes = rootNode.findAccessibilityNodeInfosByText("Search here")
                            nodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            nodes.forEach { it.recycle() }
                            return true
                        }
                        KeyEvent.KEYCODE_POUND -> {
                            var nodes = rootNode.findAccessibilityNodeInfosByText("Re-center map to your location")
                            if (nodes.isEmpty()) {
                                nodes = rootNode.findAccessibilityNodeInfosByText("Enter compass mode")
                            }
                            nodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            nodes.forEach { it.recycle() }
                            return true
                        }
                    }
                }
                Screen.UNKNOWN -> {
                }
            }
        }
        return false
    }

    private fun hasNodeWithText(rootNode: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        Log.d("BetterDpad", "Nodes found with text ${text}: ${nodes.size}")
        val found = nodes.isNotEmpty()
        nodes.forEach { it.recycle() }
        return found
    }
}
