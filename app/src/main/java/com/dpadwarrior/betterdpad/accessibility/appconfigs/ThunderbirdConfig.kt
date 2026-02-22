package com.dpadwarrior.betterdpad.accessibility.appconfigs

import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

class ThunderbirdConfig : AppAccessibilityConfig() {

    enum class Screen {
        INBOX_LIST,
        UNKNOWN
    }

    override val packageName: String = "net.thunderbird.android"

    fun detectScreen(rootNode: AccessibilityNodeInfo): Screen {
        if (hasNodeWithText(rootNode, "Compose")) return Screen.INBOX_LIST
        return Screen.UNKNOWN
    }

    override fun onKeyEvent(
        event: KeyEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        if (rootNode == null) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (detectScreen(rootNode)) {
                Screen.INBOX_LIST -> {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_POUND -> {
                            val nodes = rootNode.findAccessibilityNodeInfosByText("Compose")
                            nodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            nodes.forEach { it.recycle() }
                            return true
                        }
                        KeyEvent.KEYCODE_0 -> {
                            val nodes = rootNode.findAccessibilityNodeInfosByText("More options")
                            Log.d("BetterDpad", "More options nodes found: ${nodes.size}")
                            nodes.firstOrNull()?.let { node ->
                                Log.d("BetterDpad", "Node: className=${node.className}, " +
                                        "contentDescription=${node.contentDescription}, " +
                                        "isFocusable=${node.isFocusable}, " +
                                        "isClickable=${node.isClickable}, " +
                                        "isFocused=${node.isFocused}, " +
                                        "isVisibleToUser=${node.isVisibleToUser}")
                                val result = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                                Log.d("BetterDpad", "ACTION_FOCUS result: $result, isFocused after: ${node.isFocused}")

                                // Refresh node and check again
                                node.refresh()
                                Log.d("BetterDpad", "After refresh - isFocused: ${node.isFocused}")
                            }
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
        val found = nodes.isNotEmpty()
        nodes.forEach { it.recycle() }
        return found
    }
}
