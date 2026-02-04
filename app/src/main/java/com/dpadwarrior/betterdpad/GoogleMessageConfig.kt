package com.dpadwarrior.betterdpad

import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo

class GoogleMessageConfig : AppAccessibilityConfig() {

    enum class Screen {
        CONVERSATION_LIST,
        CONVERSATION,
        UNKNOWN
    }

    override val packageName: String = "com.google.android.apps.messaging"

    fun detectScreen(rootNode: AccessibilityNodeInfo): Screen {
        if (hasNodeWithText(rootNode, "Start chat")) return Screen.CONVERSATION_LIST
        if (hasNodeWithText(rootNode, "Show attach emoji and stickers screen")) return Screen.CONVERSATION
        return Screen.UNKNOWN
    }

    override fun onKeyEvent(
        event: KeyEvent,
        rootNode: AccessibilityNodeInfo?
    ): Boolean {
        if (rootNode == null) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (detectScreen(rootNode)) {
                Screen.CONVERSATION_LIST -> {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_POUND -> {
                            val startChatButton = findStartChatButton(rootNode)
                            startChatButton?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            startChatButton?.recycle()

                            return true
                        }
                        KeyEvent.KEYCODE_0 -> {
                            val nodes = rootNode.findAccessibilityNodeInfosByText("Search messages")
                            nodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                            nodes.forEach { it.recycle() }
                            return true
                        }
                    }
                }
                Screen.CONVERSATION -> {
//                    when (event.keyCode) {
//                        KeyEvent.KEYCODE_POUND -> {
//                            val startChatButton = findStartChatButton(rootNode)
//                            startChatButton?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
//                            startChatButton?.recycle()
//
//                            return true
//                        }
//                    }
//                    return true
                }
                Screen.UNKNOWN -> {
//                    return true
                }
            }

        }
        return false
    }

    private fun findStartChatButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText("Start chat")
        for (node in nodes) {
            if (node.className == "android.widget.Button") {
                return node
            }
        }
        return null
    }

    private fun hasNodeWithText(rootNode: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        val found = nodes.isNotEmpty()
        nodes.forEach { it.recycle() }
        return found
    }

    private fun hasNodeWithId(rootNode: AccessibilityNodeInfo, viewId: String): Boolean {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        val found = nodes.isNotEmpty()
        nodes.forEach { it.recycle() }
        return found
    }
}
