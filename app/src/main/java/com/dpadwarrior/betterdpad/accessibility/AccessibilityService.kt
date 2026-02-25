package com.dpadwarrior.betterdpad.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dpadwarrior.betterdpad.BetterDpad
import com.dpadwarrior.betterdpad.accessibility.appconfigs.AppConfigLoader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SuppressLint("AccessibilityPolicy")
class BetterDpadAccessibilityService : AccessibilityService() {

    companion object {
        // Set to true while a key binding dialog is open so all keys pass through to the UI.
        @Volatile var isCapturingKey = false
    }

    private var lastFocusedViewInfo: String? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appEnabled = AtomicBoolean(true)
    private val debugModeEnabled = AtomicBoolean(false)
    private val jumpToFirstKeyCode = AtomicReference<Int?>(null)
    private val jumpToLastKeyCode = AtomicReference<Int?>(null)
    private val jumpToFabKeyCode = AtomicReference<Int?>(null)

    private val appConfigs = AppConfigLoader.configs

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                event.source?.let { source ->
                    val currentFocusedViewInfo = "App: ${source.packageName}, " +
                            "View: ${source.className}, " +
                            "Content-Description: ${source.contentDescription}, " +
                            "ID: ${source.viewIdResourceName}"

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
                // TODO: will be needed for app-specific actions
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        // Pass all keys through when the app is disabled.
        if (!appEnabled.get()) {
            Log.d("BetterDpad", "App is disabled. Skipping")
            return super.onKeyEvent(event)
        }

        // Pass all keys through while the user is configuring a binding in the UI.
        if (isCapturingKey) {
            Log.d("BetterDpad", "isCapturingKey returns ${isCapturingKey}. Skipping interception")
            return super.onKeyEvent(event)
        }

        rootInActiveWindow?.let { rootNode ->
            // Should not intercept if user is interacting with system UI
            if (rootNode.packageName == "com.android.systemui") {
                Log.d("BetterDpad", "System UI is active. Skipping interception")
                return super.onKeyEvent(event)
            }

            // Don't intercept while the soft keyboard is active (editable text field has focus).
            val inputFocusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            val isKeyboardActive = inputFocusedNode?.isEditable == true
            inputFocusedNode?.recycle()
            if (isKeyboardActive) {
                Log.d("BetterDpad", "Keyboard is active. Skipping interception")
                rootNode.recycle()
                return super.onKeyEvent(event)
            }

            val appConfig = appConfigs[rootNode.packageName?.toString()]

            if (event.action == KeyEvent.ACTION_DOWN) {
                Log.d("BetterDpad", "Input event received. Key code: ${event.keyCode}")

                // Debug dump takes priority when STAR is pressed and debug mode is on.
                if (event.keyCode == KeyEvent.KEYCODE_STAR && debugModeEnabled.get()) {
                    Log.d("BetterDpad", "--- Dumping View Hierarchy ---")
                    logViewHierarchy(rootNode, 0)
                    Log.d("BetterDpad", "--- End of View Hierarchy ---")
                    rootNode.recycle()
                    return true
                }

                // Jump to first focusable element (per-app override, then generic tree-walk).
                val firstKey = jumpToFirstKeyCode.get()
                if (firstKey != null && event.keyCode == firstKey) {
                    val firstFocusableNode = appConfig?.findFirstFocusable(rootNode)
                        ?: findFirstFocusable(rootNode)
                    Log.d("BetterDpad", "First focusable node found: $firstFocusableNode")
                    firstFocusableNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Jump to last focusable element (per-app override, then generic tree-walk).
                val lastKey = jumpToLastKeyCode.get()
                if (lastKey != null && event.keyCode == lastKey) {
                    val lastFocusableNode = appConfig?.findLastFocusable(rootNode)
                        ?: findLastFocusable(rootNode)
                    Log.d("BetterDpad", "Last focusable node found: $lastFocusableNode")
                    lastFocusableNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Jump to FAB (per-app).
                val fabKey = jumpToFabKeyCode.get()
                if (fabKey != null && event.keyCode == fabKey) {
                    val handled = appConfig?.focusFab(rootNode) ?: false
                    rootNode.recycle()
                    if (handled) return true
                    return super.onKeyEvent(event)
                }
            }

            rootNode.recycle()
        }

        return super.onKeyEvent(event)
    }

    private fun findFirstFocusable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocusable) return node
        for (i in 0 until node.childCount) {
            val result = node.getChild(i)?.let { findFirstFocusable(it) }
            if (result != null) return result
        }
        return null
    }

    private fun findLastFocusable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var last: AccessibilityNodeInfo? = if (node.isFocusable) node else null
        for (i in 0 until node.childCount) {
            val childLast = node.getChild(i)?.let { findLastFocusable(it) }
            if (childLast != null) last = childLast
        }
        return last
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        val prefs = (application as BetterDpad).preferences
        serviceScope.launch { prefs.isAppEnabled.collect { appEnabled.set(it) } }
        serviceScope.launch { prefs.isDebugModeEnabled.collect { debugModeEnabled.set(it) } }
        serviceScope.launch { prefs.jumpToFirstKeyCode.collect { jumpToFirstKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToLastKeyCode.collect { jumpToLastKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToFabKeyCode.collect { jumpToFabKeyCode.set(it) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onInterrupt() {
    }
}
