package com.dpadwarrior.betterdpad.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.dpadwarrior.betterdpad.BetterDpad
import com.dpadwarrior.betterdpad.accessibility.appconfigs.AppAccessibilityConfig
import com.dpadwarrior.betterdpad.accessibility.appconfigs.AppConfigLoader
import com.dpadwarrior.betterdpad.shizuku.ShizukuKeyInjector
import com.dpadwarrior.betterdpad.shizuku.ShizukuState
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
    private val focusHighlightEnabled = AtomicBoolean(false)
    private val jumpToFirstKeyCode = AtomicReference<Int?>(null)
    private val jumpToLastKeyCode = AtomicReference<Int?>(null)
    private val jumpToFabKeyCode = AtomicReference<Int?>(null)
    private val dpadUpKeyCode = AtomicReference<Int?>(null)
    private val dpadDownKeyCode = AtomicReference<Int?>(null)
    private val dpadLeftKeyCode = AtomicReference<Int?>(null)
    private val dpadRightKeyCode = AtomicReference<Int?>(null)
    private val dpadSelectKeyCode = AtomicReference<Int?>(null)
    private val inputModeModifierKeyCode = AtomicReference<Int?>(null)
    private val isInputModeModifierDown = AtomicBoolean(false)

    private val appConfigs = AppConfigLoader.configs
    private val focusHighlightOverlay by lazy { FocusHighlightOverlay(this) }
    private val shizukuKeyInjector: ShizukuKeyInjector
        get() = (application as BetterDpad).shizukuKeyInjector

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
                    updateFocusHighlight(source)
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
            // Bringing the newly-focused item into view can scroll after the FOCUSED event
            // already fired, leaving the overlay's cached bounds stale mid-scroll. Resync it
            // to wherever the focused node actually is now.
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                refreshFocusHighlight()
            }
        }
    }

    private fun refreshFocusHighlight() {
        rootInActiveWindow?.let { rootNode ->
            rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focusedNode ->
                updateFocusHighlight(focusedNode)
                focusedNode.recycle()
            }
            rootNode.recycle()
        }
    }

    private fun updateFocusHighlight(source: AccessibilityNodeInfo) {
        if (!appEnabled.get() || !focusHighlightEnabled.get()) {
            focusHighlightOverlay.hide()
            return
        }
        val bounds = Rect()
        source.getBoundsInScreen(bounds)
        focusHighlightOverlay.show(bounds)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        // Track the input-mode modifier's held state unconditionally (even while keyboard is
        // active) so a mapped key pressed alongside it can override the keyboard-active skip
        // below. Never consumed here - its own down/up always passes through for normal typing.
        val modifierKey = inputModeModifierKeyCode.get()
        if (modifierKey != null && event.keyCode == modifierKey) {
            isInputModeModifierDown.set(event.action == KeyEvent.ACTION_DOWN)
        }

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
            // TODO: this logic needs more refinement. Extract this into an utility function?
            val inputFocusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            val isKeyboardActive = inputFocusedNode?.isEditable == true
            inputFocusedNode?.recycle()
            if (isKeyboardActive) {
                // Modifier+mapped-key still fires its D-pad action even while a text field is
                // focused, so the user can navigate without leaving the input.
                if (event.action == KeyEvent.ACTION_DOWN && isInputModeModifierDown.get()) {
                    val dpadKeyEvent = dpadKeyEventFor(event.keyCode)
                    if (dpadKeyEvent != null) {
                        // At the start/end (or first/last line) of the text, cursor movement
                        // would be a no-op - move accessibility focus to the previous/next
                        // element instead so the user doesn't have to exit the field some other
                        // way first.
                        if (tryExitInputAtBoundary(rootNode, dpadKeyEvent)) {
                            rootNode.recycle()
                            return true
                        }
                        if (shizukuKeyInjector.state.value == ShizukuState.READY) {
                            Log.d("BetterDpad", "Modifier override while keyboard active: $dpadKeyEvent")
                            shizukuKeyInjector.sendKeyEvent(dpadKeyEvent)
                            rootNode.recycle()
                            return true
                        }
                    }
                }
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
                    val firstFocusableNode =
                        appConfig?.getElementOverride(AppAccessibilityConfig.ElementType.FIRST, rootNode)
                            ?: findFirstFocusable(rootNode)
                    Log.d("BetterDpad", "First focusable node found: $firstFocusableNode")
                    firstFocusableNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    rootNode.recycle()
                    return true
                }

                // Jump to last focusable element (per-app override, then generic tree-walk).
                val lastKey = jumpToLastKeyCode.get()
                if (lastKey != null && event.keyCode == lastKey) {
                    val lastFocusableNode =
                        appConfig?.getElementOverride(AppAccessibilityConfig.ElementType.LAST, rootNode)
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

                // Remap an arbitrary key to a real dpad key press, injected via Shizuku. This
                // works even in apps/screens with no proper accessibility focus tree (custom
                // UIs, games), unlike a focusSearch()-based approach.
                val dpadKeyEvent = dpadKeyEventFor(event.keyCode)
                if (dpadKeyEvent != null && shizukuKeyInjector.state.value == ShizukuState.READY) {
                    shizukuKeyInjector.sendKeyEvent(dpadKeyEvent)
                    rootNode.recycle()
                    return true
                }
            }

            rootNode.recycle()
        }

        return super.onKeyEvent(event)
    }

    private fun dpadKeyEventFor(keyCode: Int): Int? = when (keyCode) {
        dpadUpKeyCode.get() -> KeyEvent.KEYCODE_DPAD_UP
        dpadDownKeyCode.get() -> KeyEvent.KEYCODE_DPAD_DOWN
        dpadLeftKeyCode.get() -> KeyEvent.KEYCODE_DPAD_LEFT
        dpadRightKeyCode.get() -> KeyEvent.KEYCODE_DPAD_RIGHT
        dpadSelectKeyCode.get() -> KeyEvent.KEYCODE_DPAD_CENTER
        else -> null
    }

    private enum class ExitDirection { PREVIOUS, NEXT }

    /**
     * If the focused text field's cursor is already at the boundary the given dpad direction
     * would move past - start/end for left/right, first/last line for up/down - moves
     * accessibility focus to the adjacent focusable element in tree order instead of doing
     * nothing. A field with no newlines is always at both the first and last line, so this
     * degrades correctly for single-line fields. Returns false if not at that boundary yet, or
     * if selection info isn't available (e.g. a non-text-editing field briefly reports focus).
     */
    private fun tryExitInputAtBoundary(rootNode: AccessibilityNodeInfo, dpadKeyEvent: Int): Boolean {
        val direction = when (dpadKeyEvent) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> ExitDirection.PREVIOUS
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> ExitDirection.NEXT
            else -> return false
        }

        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        try {
            val text = focusedNode.text?.toString() ?: ""
            val selectionStart = focusedNode.textSelectionStart
            val selectionEnd = focusedNode.textSelectionEnd
            if (selectionStart < 0 || selectionEnd < 0) return false

            val atBoundary = when (dpadKeyEvent) {
                KeyEvent.KEYCODE_DPAD_LEFT -> selectionStart == 0 && selectionEnd == 0
                KeyEvent.KEYCODE_DPAD_RIGHT -> selectionStart == text.length && selectionEnd == text.length
                KeyEvent.KEYCODE_DPAD_UP ->
                    !text.substring(0, selectionStart.coerceAtMost(text.length)).contains('\n')
                KeyEvent.KEYCODE_DPAD_DOWN ->
                    !text.substring(selectionEnd.coerceAtMost(text.length)).contains('\n')
                else -> false
            }
            if (!atBoundary) return false

            val focusables = mutableListOf<AccessibilityNodeInfo>()
            collectFocusables(rootNode, focusables)
            val index = focusables.indexOfFirst { it == focusedNode }
            val target = when (direction) {
                ExitDirection.PREVIOUS -> focusables.getOrNull(index - 1)
                ExitDirection.NEXT -> focusables.getOrNull(index + 1)
            }
            Log.d("BetterDpad", "Exiting input field ($direction) to: $target")
            return target?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ?: false
        } finally {
            focusedNode.recycle()
        }
    }

    private fun collectFocusables(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (node.isFocusable) out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectFocusables(it, out) }
        }
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
        serviceScope.launch { prefs.isAppEnabled.collect { enabled ->
            appEnabled.set(enabled)
            if (!enabled) focusHighlightOverlay.hide()
        } }
        serviceScope.launch { prefs.isDebugModeEnabled.collect { debugModeEnabled.set(it) } }
        serviceScope.launch { prefs.isFocusHighlightEnabled.collect { enabled ->
            focusHighlightEnabled.set(enabled)
            if (!enabled) focusHighlightOverlay.hide()
        } }
        serviceScope.launch { prefs.jumpToFirstKeyCode.collect { jumpToFirstKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToLastKeyCode.collect { jumpToLastKeyCode.set(it) } }
        serviceScope.launch { prefs.jumpToFabKeyCode.collect { jumpToFabKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadUpKeyCode.collect { dpadUpKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadDownKeyCode.collect { dpadDownKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadLeftKeyCode.collect { dpadLeftKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadRightKeyCode.collect { dpadRightKeyCode.set(it) } }
        serviceScope.launch { prefs.dpadSelectKeyCode.collect { dpadSelectKeyCode.set(it) } }
        serviceScope.launch { prefs.inputModeModifierKeyCode.collect { inputModeModifierKeyCode.set(it) } }
    }

    override fun onDestroy() {
        super.onDestroy()
        focusHighlightOverlay.hide()
        serviceScope.cancel()
    }

    override fun onInterrupt() {
    }
}
