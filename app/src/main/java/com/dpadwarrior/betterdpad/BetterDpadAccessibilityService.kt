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
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.source?.let { source ->
                    val windowStateChangeInfo = "App: ${source.packageName}, " +
                            "Source: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"
                    Log.d("BetterDpad", "Window state changed: $windowStateChangeInfo")
                    source.recycle()
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_POUND) {
            val rootNode = rootInActiveWindow
            if (rootNode?.packageName == "com.google.android.apps.messaging") {
                val startChatButton = findStartChatButton(rootNode)
                startChatButton?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                startChatButton?.recycle()
            }
            rootNode?.recycle()
        }
        return super.onKeyEvent(event)
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

    override fun onInterrupt() {
        // Not needed for this implementation
    }
}
