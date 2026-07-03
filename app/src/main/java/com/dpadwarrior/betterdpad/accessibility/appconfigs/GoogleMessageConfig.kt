package com.dpadwarrior.betterdpad.accessibility.appconfigs

import android.view.accessibility.AccessibilityNodeInfo

class GoogleMessageConfig : AppAccessibilityConfig() {

    enum class Screen {
        CONVERSATION_LIST,
        CONVERSATION,
        UNKNOWN
    }

    override val packageName: String = "com.google.android.apps.messaging"

    override val fabLabel = "Start chat"

    // TODO: turn this into a more standardized function for all configs
    // Will be used to define overrides for first/last elements + actions
    fun detectScreen(rootNode: AccessibilityNodeInfo): Screen? {
        if (hasNodeWithText(rootNode, "Start chat")) return Screen.CONVERSATION_LIST
        if (hasNodeWithText(rootNode, "Show attach emoji and stickers screen")) return Screen.CONVERSATION
        return null
    }

    override fun getElementOverride(
        elementType: ElementType,
        rootNode: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        when (detectScreen(rootNode)) {
            Screen.CONVERSATION_LIST -> when (elementType) {
                ElementType.FIRST -> {}
                ElementType.LAST -> {}
            }
            Screen.CONVERSATION -> when (elementType) {
                ElementType.FIRST -> {}
                ElementType.LAST -> {
                    val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.apps.messaging:id/Compose:Draft:Send")
                    val node = nodes.firstOrNull()
                    nodes.drop(1).forEach { it.recycle() }
                    return node
                }
            }
            else -> {}
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
