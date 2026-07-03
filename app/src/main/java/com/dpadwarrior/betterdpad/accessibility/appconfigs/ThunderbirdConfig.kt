package com.dpadwarrior.betterdpad.accessibility.appconfigs

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

    override fun getElementOverride(
        elementType: ElementType,
        rootNode: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        when (detectScreen(rootNode)) {
            Screen.INBOX_LIST -> {
                when (elementType) {
//                    ElementType.FIRST -> {
//                        val nodes = rootNode.findAccessibilityNodeInfosByText("Compose")
//                        nodes.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
//                        nodes.forEach { it.recycle() }
//                        return true
//                    }
//                    ElementType.LAST -> {
//                        val nodes = rootNode.findAccessibilityNodeInfosByText("More options")
//                        Log.d("BetterDpad", "More options nodes found: ${nodes.size}")
//                        nodes.forEach { it.recycle() }
//                        return true
//                    }
                    else -> {}
                }
            }
            Screen.UNKNOWN -> {
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
}
