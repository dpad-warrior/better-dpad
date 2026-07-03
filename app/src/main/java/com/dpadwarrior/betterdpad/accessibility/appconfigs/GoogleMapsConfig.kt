package com.dpadwarrior.betterdpad.accessibility.appconfigs

import android.view.accessibility.AccessibilityNodeInfo

class GoogleMapsConfig : AppAccessibilityConfig() {

    override val packageName: String = "com.google.android.apps.maps"

    override val fabLabel = "Re-center map to your location"

    override fun getElementOverride(
        elementType: ElementType,
        rootNode: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        if (rootNode == null) return null
        return when (elementType) {
            ElementType.FIRST -> rootNode.findAccessibilityNodeInfosByText("Search here").firstOrNull()
            ElementType.LAST -> rootNode.findAccessibilityNodeInfosByText("Contribute").firstOrNull()
        }
    }
}
