package com.dpadwarrior.betterdpad

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

@SuppressLint("AccessibilityPolicy")
class BetterDpadAccessibilityService : AccessibilityService() {

    private var lastFocusedViewInfo: String? = null

    private val appConfigs: Map<String, AppAccessibilityConfig> = listOf(
        GoogleMessageConfig(),
        GoogleMapsConfig()
    ).associateBy { it.packageName }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                event.source?.let { source ->
                    val currentFocusedViewInfo = "App: ${source.packageName}, " +
                            "View: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"

                    Log.d("BetterDpad", "Previous focused view: $lastFocusedViewInfo")
                    Log.d("BetterDpad", "Current focused view: $currentFocusedViewInfo")

                    lastFocusedViewInfo = currentFocusedViewInfo
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                event.source?.let { source ->
                    val clickedViewInfo = "App: ${source.packageName}, " +
                            "View: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"
                    Log.d("BetterDpad", "Clicked view: $clickedViewInfo")
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val rootNode = rootInActiveWindow ?: return
                val pkg = rootNode.packageName?.toString()
                if (pkg != null) {
                    appConfigs[pkg]?.onAccessibilityEvent(event, rootNode)
                }
                rootNode.recycle()
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        rootInActiveWindow?.let { rootNode ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_STAR) {
                Log.d("BetterDpad", "--- Dumping View Hierarchy ---")
                logViewHierarchy(rootNode, 0)
                Log.d("BetterDpad", "--- End of View Hierarchy ---")
                rootNode.recycle()
                return true
            }

            val pkg = rootNode.packageName?.toString()
            if (pkg != null) {
                Log.d("BetterDpad", "Package name: $pkg")
                val config = appConfigs[pkg]
                if (config != null) {
                    Log.d("BetterDpad", "Config found: ${config.packageName} ${event.keyCode} ${event.action}")
                    val handled = config.onKeyEvent(event, rootNode)
                    rootNode.recycle()
                    if (handled) return true
                } else {
                    rootNode.recycle()
                }
            } else {
                rootNode.recycle()
            }
        }

        return super.onKeyEvent(event)
    }

    private fun logViewHierarchy(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return
        val indent = "  ".repeat(depth)
        val nodeInfo = "View: ${node.className}, " +
                "Content-Description: ${node.contentDescription}, " +
                "ID: [${node.viewIdResourceName}], " +
                "Clickable: ${node.isClickable}, " +
                "Focusable: ${node.isFocusable}"
        Log.d("BetterDpad", "$indent$nodeInfo")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            logViewHierarchy(child, depth + 1)
            child?.recycle()
        }
    }

    override fun onInterrupt() {
    }
}
